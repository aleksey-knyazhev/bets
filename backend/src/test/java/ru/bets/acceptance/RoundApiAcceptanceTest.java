package ru.bets.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.OK;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import ru.bets.model.CalculationResult;

@Tag("acceptance")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class RoundApiAcceptanceTest {

    private static final String EMPTY_ROUND_MESSAGE =
            "Ошибка: расчет невозможен. Участники еще не сгенерировали броски!";
    private static final String INVALID_PLAYER_MESSAGE = "playerId должен быть равен 1 или 2";

    @Container
    private static final KafkaContainer KAFKA = new KafkaContainer(
            DockerImageName.parse("apache/kafka-native:3.8.0")
    );

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${local.server.port}")
    private int port;

    @Test
    void completesRoundLifecycleThroughRestApiAndKafka() throws Exception {
        int initialRound = currentRound();
        assertThat(initialRound).isPositive();

        assertCalculationRejectedUntilBothPlayersHaveRolls();

        List<Integer> firstPartitionRolls = generateRolls(1);
        assertCalculationRejectedUntilBothPlayersHaveRolls();

        List<Integer> secondPartitionRolls = generateRolls(2);
        CalculationResult actualResult = calculate();
        assertThat(actualResult)
                .isEqualTo(expectedResult(firstPartitionRolls, secondPartitionRolls));

        ApiResponse<Integer> nextRoundResponse = post("/api/next-round", Integer.class);
        assertThat(nextRoundResponse.statusCode()).isEqualTo(OK.value());
        assertThat(nextRoundResponse.body()).isEqualTo(initialRound + 1);
        assertThat(currentRound()).isEqualTo(initialRound + 1);
        assertCalculationRejectedUntilBothPlayersHaveRolls();
    }

    @Test
    void rejectsGenerationForUnknownPlayerId() throws Exception {
        ApiResponse<JsonNode> response = post("/api/generate/3", JsonNode.class);

        assertThat(response.statusCode()).isEqualTo(BAD_REQUEST.value());
        assertThat(response.body().path("message").asText()).isEqualTo(INVALID_PLAYER_MESSAGE);
    }

    @DynamicPropertySource
    static void configureKafka(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
    }

    private int currentRound() throws Exception {
        ApiResponse<Integer> response = get("/api/round", Integer.class);

        assertThat(response.statusCode()).isEqualTo(OK.value());
        return response.body();
    }

    private List<Integer> generateRolls(int playerId) throws Exception {
        ApiResponse<Integer[]> response = post("/api/generate/" + playerId, Integer[].class);

        assertThat(response.statusCode()).isEqualTo(OK.value());
        Integer[] body = response.body();
        assertThat(body).isNotNull().hasSize(10);

        List<Integer> rolls = List.of(body);
        assertThat(rolls).allSatisfy(roll -> assertThat(roll).isBetween(1, 6));
        return rolls;
    }

    private void assertCalculationRejectedUntilBothPlayersHaveRolls() throws Exception {
        ApiResponse<JsonNode> response = get("/api/calculate", JsonNode.class);

        assertThat(response.statusCode()).isEqualTo(BAD_REQUEST.value());
        assertThat(response.body().path("message").asText()).isEqualTo(EMPTY_ROUND_MESSAGE);
    }

    private CalculationResult calculate() throws Exception {
        ApiResponse<CalculationResult> response = get("/api/calculate", CalculationResult.class);

        assertThat(response.statusCode()).isEqualTo(OK.value());
        return response.body();
    }

    private CalculationResult expectedResult(List<Integer> firstRolls, List<Integer> secondRolls) {
        Integer firstWinningStep = winningStep(firstRolls);
        Integer secondWinningStep = winningStep(secondRolls);

        if (firstWinningStep == null && secondWinningStep == null) {
            return new CalculationResult("Ничья", null);
        }
        if (firstWinningStep != null && firstWinningStep.equals(secondWinningStep)) {
            return new CalculationResult("Ничья", firstWinningStep);
        }
        if (secondWinningStep == null
                || (firstWinningStep != null && firstWinningStep < secondWinningStep)) {
            return new CalculationResult("Партиция 0", firstWinningStep);
        }
        return new CalculationResult("Партиция 1", secondWinningStep);
    }

    private Integer winningStep(List<Integer> rolls) {
        int score = 0;
        for (int index = 0; index < rolls.size(); index++) {
            score += rolls.get(index);
            if (score >= 30) {
                return index + 1;
            }
        }
        return null;
    }

    private <T> ApiResponse<T> get(String path, Class<T> responseType) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(apiUri(path)).GET().build();
        return send(request, responseType);
    }

    private <T> ApiResponse<T> post(String path, Class<T> responseType) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(apiUri(path))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        return send(request, responseType);
    }

    private <T> ApiResponse<T> send(HttpRequest request, Class<T> responseType) throws Exception {
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        T body = objectMapper.readValue(response.body(), responseType);
        return new ApiResponse<>(response.statusCode(), body);
    }

    private URI apiUri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    private record ApiResponse<T>(int statusCode, T body) {
    }
}
