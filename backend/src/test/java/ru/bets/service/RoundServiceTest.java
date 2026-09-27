package ru.bets.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import ru.bets.domain.ScoreCalculator;
import ru.bets.exception.RoundValidationException;
import ru.bets.model.CalculationResult;
import ru.bets.model.PlayerRolls;

class RoundServiceTest {

    @Test
    void generateWritesRollsToPartitionForPlayer() {
        FakeRoundState roundState = new FakeRoundState(3);
        FakeRollGenerator rollGenerator = new FakeRollGenerator(List.of(1, 2, 3, 4, 5, 6, 1, 2, 3, 4));
        CapturingDiceRollWriter writer = new CapturingDiceRollWriter();
        RoundService service = new RoundService(
                roundState,
                rollGenerator,
                writer,
                (partition, round) -> new PlayerRolls(partition, List.of()),
                new ScoreCalculator()
        );

        List<Integer> rolls = service.generate(2);

        assertThat(rolls).isEqualTo(rollGenerator.rolls);
        assertThat(writer.round).isEqualTo(3);
        assertThat(writer.partition).isEqualTo(1);
        assertThat(writer.rolls).isEqualTo(rollGenerator.rolls);
    }

    @Test
    void generateRejectsUnknownPlayer() {
        RoundService service = new RoundService(
                new FakeRoundState(1),
                new FakeRollGenerator(List.of()),
                new CapturingDiceRollWriter(),
                (partition, round) -> new PlayerRolls(partition, List.of()),
                new ScoreCalculator()
        );

        assertThatThrownBy(() -> service.generate(3))
                .isInstanceOf(RoundValidationException.class)
                .hasMessage("playerId должен быть равен 1 или 2");
    }

    @Test
    void calculateReadsBothPartitionsForCurrentRound() {
        RecordingDiceRollReader reader = new RecordingDiceRollReader();
        RoundService service = new RoundService(
                new FakeRoundState(7),
                new FakeRollGenerator(List.of()),
                new CapturingDiceRollWriter(),
                reader,
                new ScoreCalculator()
        );

        CalculationResult result = service.calculate();

        assertThat(result.winner()).isEqualTo("Партиция 0");
        assertThat(result.winningStep()).isEqualTo(5);
        assertThat(reader.requests).containsExactlyInAnyOrder("0:7", "1:7");
    }

    private static final class FakeRoundState implements RoundState {

        private int round;

        private FakeRoundState(int round) {
            this.round = round;
        }

        @Override
        public int currentRound() {
            return round;
        }

        @Override
        public int nextRound() {
            return ++round;
        }
    }

    private static final class FakeRollGenerator implements RollGenerator {

        private final List<Integer> rolls;

        private FakeRollGenerator(List<Integer> rolls) {
            this.rolls = rolls;
        }

        @Override
        public List<Integer> generate(int count) {
            return rolls;
        }
    }

    private static final class CapturingDiceRollWriter implements DiceRollWriter {

        private int round;
        private int partition;
        private List<Integer> rolls = List.of();

        @Override
        public void writeRolls(int round, int partition, List<Integer> rolls) {
            this.round = round;
            this.partition = partition;
            this.rolls = rolls;
        }
    }

    private static final class RecordingDiceRollReader implements DiceRollReader {

        private final List<String> requests = new ArrayList<>();

        @Override
        public PlayerRolls readRoundRolls(int partition, int round) {
            requests.add(partition + ":" + round);
            if (partition == 0) {
                return new PlayerRolls(0, List.of(6, 6, 6, 6, 6, 1, 1, 1, 1, 1));
            }
            return new PlayerRolls(1, List.of(1, 1, 1, 1, 1, 1, 1, 1, 1, 1));
        }
    }
}
