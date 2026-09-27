package ru.bets.domain;

import java.util.List;

import org.springframework.stereotype.Component;

import ru.bets.exception.RoundValidationException;
import ru.bets.model.CalculationResult;
import ru.bets.model.PlayerRolls;

@Component
public class ScoreCalculator {

    public static final int ROLLS_PER_PARTITION = 10;
    public static final int WINNING_SCORE = 30;
    public static final String EMPTY_ROUND_MESSAGE = "Ошибка: расчет невозможен. Участники еще не сгенерировали броски!";

    public CalculationResult calculate(PlayerRolls firstPartition, PlayerRolls secondPartition) {
        validateCurrentRound(firstPartition, secondPartition);

        PlayerScore firstScore = score(firstPartition);
        PlayerScore secondScore = score(secondPartition);

        Integer firstStep = firstScore.winningStep();
        Integer secondStep = secondScore.winningStep();

        if (firstStep == null && secondStep == null) {
            return new CalculationResult("Ничья", null);
        }
        if (firstStep != null && secondStep != null) {
            if (firstStep.equals(secondStep)) {
                return new CalculationResult("Ничья", firstStep);
            }
            return firstStep < secondStep
                    ? new CalculationResult(firstScore.name(), firstStep)
                    : new CalculationResult(secondScore.name(), secondStep);
        }
        return firstStep != null
                ? new CalculationResult(firstScore.name(), firstStep)
                : new CalculationResult(secondScore.name(), secondStep);
    }

    private void validateCurrentRound(PlayerRolls firstPartition, PlayerRolls secondPartition) {
        if (firstPartition.rolls().size() < ROLLS_PER_PARTITION
                || secondPartition.rolls().size() < ROLLS_PER_PARTITION) {
            throw new RoundValidationException(EMPTY_ROUND_MESSAGE);
        }
    }

    private PlayerScore score(PlayerRolls playerRolls) {
        List<Integer> rolls = playerRolls.rolls();
        int sum = 0;
        Integer winningStep = null;
        for (int index = 0; index < rolls.size(); index++) {
            sum += rolls.get(index);
            if (sum >= WINNING_SCORE) {
                winningStep = index + 1;
                break;
            }
        }
        return new PlayerScore(playerRolls.displayName(), winningStep);
    }

    private record PlayerScore(String name, Integer winningStep) {
    }
}
