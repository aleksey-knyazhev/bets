package ru.bets.infrastructure.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.bets.config.KafkaTopicConfig.TOPIC_NAME;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;

@Testcontainers(disabledWithoutDocker = true)
class KafkaDiceRollIntegrationTest {

    @Container
    private static final KafkaContainer KAFKA = new KafkaContainer(
            DockerImageName.parse("apache/kafka-native:3.8.0")
    );

    @Test
    void writerAndReaderPreservePartitionRoundAndLatestTenRolls() throws Exception {
        try (var admin = AdminClient.create(Map.of("bootstrap.servers", KAFKA.getBootstrapServers()))) {
            admin.createTopics(List.of(new NewTopic(TOPIC_NAME, 2, (short) 1)))
                    .all()
                    .get(30, TimeUnit.SECONDS);
        }

        Map<String, Object> producerProperties = Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers(),
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class
        );
        var producerFactory = new DefaultKafkaProducerFactory<String, String>(producerProperties);
        var kafkaTemplate = new KafkaTemplate<>(producerFactory);

        try {
            var objectMapper = new ObjectMapper();
            var writer = new KafkaDiceRollWriter(kafkaTemplate, objectMapper);
            var reader = new KafkaDiceRollReader(
                    new KafkaStringConsumerFactory(KAFKA.getBootstrapServers()),
                    objectMapper
            );

            writer.writeRolls(1, 0, List.of(1, 2, 3, 4, 5, 6, 1, 2, 3, 4, 5, 6));
            writer.writeRolls(2, 0, List.of(6, 6, 6));
            writer.writeRolls(1, 1, List.of(2, 2, 2, 2, 2, 2, 2, 2, 2, 2));

            assertThat(reader.readRoundRolls(0, 1).rolls())
                    .containsExactly(3, 4, 5, 6, 1, 2, 3, 4, 5, 6);
            assertThat(reader.readRoundRolls(1, 1).rolls())
                    .containsExactly(2, 2, 2, 2, 2, 2, 2, 2, 2, 2);
            assertThat(reader.readRoundRolls(0, 99).rolls()).isEmpty();

            kafkaTemplate.send(TOPIC_NAME, 0, "partition-0", "not-json")
                    .get(10, TimeUnit.SECONDS);

            assertThatThrownBy(() -> reader.readRoundRolls(0, 1))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Не удалось прочитать сообщение Kafka");
        } finally {
            producerFactory.destroy();
        }
    }
}
