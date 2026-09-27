package ru.bets.infrastructure.kafka;

import static ru.bets.config.KafkaTopicConfig.TOPIC_NAME;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.springframework.stereotype.Component;

import ru.bets.domain.ScoreCalculator;
import ru.bets.model.DiceRollMessage;
import ru.bets.model.PlayerRolls;
import ru.bets.service.DiceRollReader;

@Component
public class KafkaDiceRollReader implements DiceRollReader {

    private static final Duration POLL_TIMEOUT = Duration.ofMillis(250);

    private final KafkaStringConsumerFactory consumerFactory;
    private final ObjectMapper objectMapper;

    public KafkaDiceRollReader(KafkaStringConsumerFactory consumerFactory, ObjectMapper objectMapper) {
        this.consumerFactory = consumerFactory;
        this.objectMapper = objectMapper;
    }

    @Override
    public PlayerRolls readRoundRolls(int partition, int round) {
        TopicPartition topicPartition = new TopicPartition(TOPIC_NAME, partition);

        try (var consumer = consumerFactory.createConsumer()) {
            consumer.assign(List.of(topicPartition));
            long endOffset = consumer.endOffsets(List.of(topicPartition)).getOrDefault(topicPartition, 0L);
            consumer.seek(topicPartition, 0L);

            List<Integer> rolls = readRollsForRound(consumer, topicPartition, endOffset, round);
            return new PlayerRolls(partition, takeLatestRolls(rolls));
        }
    }

    private List<Integer> readRollsForRound(
            KafkaConsumer<String, String> consumer,
            TopicPartition topicPartition,
            long endOffset,
            int round
    ) {
        List<Integer> rolls = new ArrayList<>();
        while (consumer.position(topicPartition) < endOffset) {
            var records = consumer.poll(POLL_TIMEOUT);
            records.records(topicPartition).forEach(record -> {
                DiceRollMessage message = parse(record.value());
                if (message.round() == round) {
                    rolls.add(message.value());
                }
            });
        }
        return rolls;
    }

    private List<Integer> takeLatestRolls(List<Integer> rolls) {
        return rolls.size() <= ScoreCalculator.ROLLS_PER_PARTITION
                ? rolls
                : rolls.subList(rolls.size() - ScoreCalculator.ROLLS_PER_PARTITION, rolls.size());
    }

    private DiceRollMessage parse(String value) {
        try {
            return objectMapper.readValue(value, DiceRollMessage.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Не удалось прочитать сообщение Kafka: " + value, exception);
        }
    }
}
