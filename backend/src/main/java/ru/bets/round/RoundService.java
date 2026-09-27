package ru.bets.round;

import static ru.bets.config.KafkaTopicConfig.TOPIC_NAME;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.StructuredTaskScope.Joiner;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import ru.bets.exception.RoundValidationException;

@Service
public class RoundService {

    private static final int ROLLS_PER_PLAYER = 10;
    private static final int WINNING_SCORE = 30;
    private static final String EMPTY_ROUND_MESSAGE = "Ошибка: расчет невозможен. Участники еще не сгенерировали броски!";

    private final AtomicInteger currentRound = new AtomicInteger(1);
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String bootstrapServers;

    public RoundService(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.bootstrapServers = bootstrapServers;
    }

    public int getCurrentRound() {
        return currentRound.get();
    }

    public int nextRound() {
        return currentRound.incrementAndGet();
    }

    public List<Integer> generate(int playerId) {
        if (playerId != 1 && playerId != 2) {
            throw new RoundValidationException("playerId должен быть равен 1 или 2");
        }

        int round = currentRound.get();
        int partition = playerId - 1;
        List<Integer> rolls = ThreadLocalRandom.current()
                .ints(ROLLS_PER_PLAYER, 1, 7)
                .boxed()
                .toList();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            executor.submit(() -> sendRolls(round, partition, rolls));
        }

        return rolls;
    }

    public CalculationResult calculate() {
        int round = currentRound.get();
        ensureTopicHasData();

        try (var scope = StructuredTaskScope.open(Joiner.<PlayerScore>awaitAllSuccessfulOrThrow())) {
            var playerOneTask = scope.fork(() -> readPartition(0, round));
            var playerTwoTask = scope.fork(() -> readPartition(1, round));

            scope.join();

            PlayerScore playerOne = playerOneTask.get();
            PlayerScore playerTwo = playerTwoTask.get();
            validateCurrentRound(playerOne, playerTwo);
            return determineWinner(playerOne, playerTwo);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Расчет был прерван", exception);
        } catch (RoundValidationException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new IllegalStateException("Не удалось рассчитать результат", exception);
        }
    }

    private void sendRolls(int round, int partition, List<Integer> rolls) {
        for (Integer roll : rolls) {
            try {
                String payload = objectMapper.writeValueAsString(new DiceRollMessage(round, roll));
                kafkaTemplate.send(TOPIC_NAME, partition, "player-" + (partition + 1), payload).get();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Отправка бросков была прервана", exception);
            } catch (ExecutionException | JsonProcessingException exception) {
                throw new IllegalStateException("Не удалось отправить броски в Kafka", exception);
            }
        }
    }

    private void ensureTopicHasData() {
        try (var consumer = createConsumer()) {
            TopicPartition partitionZero = new TopicPartition(TOPIC_NAME, 0);
            TopicPartition partitionOne = new TopicPartition(TOPIC_NAME, 1);
            Map<TopicPartition, Long> endOffsets = consumer.endOffsets(List.of(partitionZero, partitionOne));

            if (endOffsets.getOrDefault(partitionZero, 0L) < ROLLS_PER_PLAYER
                    || endOffsets.getOrDefault(partitionOne, 0L) < ROLLS_PER_PLAYER) {
                throw new RoundValidationException(EMPTY_ROUND_MESSAGE);
            }
        }
    }

    private PlayerScore readPartition(int partition, int round) {
        TopicPartition topicPartition = new TopicPartition(TOPIC_NAME, partition);

        try (var consumer = createConsumer()) {
            consumer.assign(List.of(topicPartition));
            long endOffset = consumer.endOffsets(List.of(topicPartition)).getOrDefault(topicPartition, 0L);
            consumer.seek(topicPartition, 0L);

            List<Integer> rolls = new ArrayList<>();
            while (consumer.position(topicPartition) < endOffset) {
                var records = consumer.poll(Duration.ofMillis(250));
                records.records(topicPartition).forEach(record -> {
                    DiceRollMessage message = parse(record.value());
                    if (message.round() == round) {
                        rolls.add(message.value());
                    }
                });
            }

            List<Integer> currentRolls = rolls.size() <= ROLLS_PER_PLAYER
                    ? rolls
                    : rolls.subList(rolls.size() - ROLLS_PER_PLAYER, rolls.size());
            return score("Партиция " + partition, currentRolls);
        }
    }

    private DiceRollMessage parse(String value) {
        try {
            return objectMapper.readValue(value, DiceRollMessage.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Не удалось прочитать сообщение Kafka: " + value, exception);
        }
    }

    private PlayerScore score(String name, List<Integer> rolls) {
        int sum = 0;
        Integer winningStep = null;
        for (int index = 0; index < rolls.size(); index++) {
            sum += rolls.get(index);
            if (sum >= WINNING_SCORE) {
                winningStep = index + 1;
                break;
            }
        }
        return new PlayerScore(name, rolls, winningStep);
    }

    private void validateCurrentRound(PlayerScore playerOne, PlayerScore playerTwo) {
        if (playerOne.rolls().size() < ROLLS_PER_PLAYER || playerTwo.rolls().size() < ROLLS_PER_PLAYER) {
            throw new RoundValidationException(EMPTY_ROUND_MESSAGE);
        }
    }

    private CalculationResult determineWinner(PlayerScore playerOne, PlayerScore playerTwo) {
        Integer firstStep = playerOne.winningStep();
        Integer secondStep = playerTwo.winningStep();

        if (firstStep == null && secondStep == null) {
            return new CalculationResult("Ничья", null);
        }
        if (firstStep != null && secondStep != null) {
            if (firstStep.equals(secondStep)) {
                return new CalculationResult("Ничья", firstStep);
            }
            return firstStep < secondStep
                    ? new CalculationResult(playerOne.name(), firstStep)
                    : new CalculationResult(playerTwo.name(), secondStep);
        }
        return firstStep != null
                ? new CalculationResult(playerOne.name(), firstStep)
                : new CalculationResult(playerTwo.name(), secondStep);
    }

    private KafkaConsumer<String, String> createConsumer() {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "bets-calculation-" + UUID.randomUUID());
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return new KafkaConsumer<>(properties);
    }

    private record PlayerScore(String name, List<Integer> rolls, Integer winningStep) {
    }
}
