package ru.bets.service;

import java.util.List;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.StructuredTaskScope.Joiner;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;

import ru.bets.exception.RoundValidationException;
import ru.bets.model.CalculationResult;
import ru.bets.model.PlayerRolls;

@Service
public class RoundService {

    private final AtomicInteger currentRound = new AtomicInteger(1);
    private final DiceRollProducer diceRollProducer;
    private final DiceRollReader diceRollReader;
    private final ScoreCalculator scoreCalculator;

    public RoundService(
            DiceRollProducer diceRollProducer,
            DiceRollReader diceRollReader,
            ScoreCalculator scoreCalculator
    ) {
        this.diceRollProducer = diceRollProducer;
        this.diceRollReader = diceRollReader;
        this.scoreCalculator = scoreCalculator;
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
                .ints(ScoreCalculator.ROLLS_PER_PARTITION, 1, 7)
                .boxed()
                .toList();

        diceRollProducer.sendRolls(round, partition, rolls);
        return rolls;
    }

    public CalculationResult calculate() {
        int round = currentRound.get();

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
