package ru.bets.service;

import static ru.bets.config.KafkaTopicConfig.TOPIC_NAME;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import ru.bets.model.DiceRollMessage;
import ru.bets.model.PlayerRolls;

@Component
public class DiceRollReader {

    private static final Duration POLL_TIMEOUT = Duration.ofMillis(250);

    private final ObjectMapper objectMapper;
    private final String bootstrapServers;

    public DiceRollReader(
            ObjectMapper objectMapper,
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers
    ) {
        this.objectMapper = objectMapper;
        this.bootstrapServers = bootstrapServers;
    }

    public PlayerRolls readRoundRolls(int partition, int round) {
        TopicPartition topicPartition = new TopicPartition(TOPIC_NAME, partition);

        try (var consumer = createConsumer()) {
            consumer.assign(List.of(topicPartition));
            long endOffset = consumer.endOffsets(List.of(topicPartition)).getOrDefault(topicPartition, 0L);
            consumer.seek(topicPartition, 0L);

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

            List<Integer> currentRolls = rolls.size() <= ScoreCalculator.ROLLS_PER_PARTITION
                    ? rolls
                    : rolls.subList(rolls.size() - ScoreCalculator.ROLLS_PER_PARTITION, rolls.size());
            return new PlayerRolls(partition, currentRolls);
        }
    }

    private DiceRollMessage parse(String value) {
        try {
            return objectMapper.readValue(value, DiceRollMessage.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Не удалось прочитать сообщение Kafka: " + value, exception);
        }
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
}
