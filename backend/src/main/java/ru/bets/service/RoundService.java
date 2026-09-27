package ru.bets.service;

import java.util.List;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.StructuredTaskScope.Joiner;

import org.springframework.stereotype.Service;

import ru.bets.domain.ScoreCalculator;
import ru.bets.exception.RoundValidationException;
import ru.bets.model.CalculationResult;
import ru.bets.model.PlayerRolls;

@Service
public class RoundService {

    private final RoundState roundState;
    private final RollGenerator rollGenerator;
    private final DiceRollWriter diceRollWriter;
    private final DiceRollReader diceRollReader;
    private final ScoreCalculator scoreCalculator;

    public RoundService(
            RoundState roundState,
            RollGenerator rollGenerator,
            DiceRollWriter diceRollWriter,
            DiceRollReader diceRollReader,
            ScoreCalculator scoreCalculator
    ) {
        this.roundState = roundState;
        this.rollGenerator = rollGenerator;
        this.diceRollWriter = diceRollWriter;
        this.diceRollReader = diceRollReader;
        this.scoreCalculator = scoreCalculator;
    }

    public int getCurrentRound() {
        return roundState.currentRound();
    }

    public int nextRound() {
        return roundState.nextRound();
    }

    public List<Integer> generate(int playerId) {
        if (playerId != 1 && playerId != 2) {
            throw new RoundValidationException("playerId должен быть равен 1 или 2");
        }

        int round = roundState.currentRound();
        int partition = playerId - 1;
        List<Integer> rolls = rollGenerator.generate(ScoreCalculator.ROLLS_PER_PARTITION);

        diceRollWriter.writeRolls(round, partition, rolls);
        return rolls;
    }

    public CalculationResult calculate() {
        int round = roundState.currentRound();

        try (var scope = StructuredTaskScope.open(Joiner.<PlayerRolls>awaitAllSuccessfulOrThrow())) {
            var partitionZeroTask = scope.fork(() -> diceRollReader.readRoundRolls(0, round));
            var partitionOneTask = scope.fork(() -> diceRollReader.readRoundRolls(1, round));

            scope.join();

            return scoreCalculator.calculate(partitionZeroTask.get(), partitionOneTask.get());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Расчет был прерван", exception);
        } catch (RoundValidationException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new IllegalStateException("Не удалось рассчитать результат", exception);
        }
    }
}
