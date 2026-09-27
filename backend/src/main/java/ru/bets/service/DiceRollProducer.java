package ru.bets.service;

import static ru.bets.config.KafkaTopicConfig.TOPIC_NAME;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import ru.bets.model.DiceRollMessage;

@Component
public class DiceRollProducer {

    private static final int SEND_TIMEOUT_SECONDS = 10;

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public DiceRollProducer(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void sendRolls(int round, int partition, List<Integer> rolls) {
        for (Integer roll : rolls) {
            sendRoll(round, partition, roll);
        }
    }

    private void sendRoll(int round, int partition, Integer roll) {
        try {
            String payload = objectMapper.writeValueAsString(new DiceRollMessage(round, roll));
            kafkaTemplate
                    .send(TOPIC_NAME, partition, "partition-" + partition, payload)
                    .get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Отправка бросков была прервана", exception);
        } catch (ExecutionException | TimeoutException | JsonProcessingException exception) {
            throw new IllegalStateException("Не удалось отправить броски в Kafka", exception);
        }
    }
}
