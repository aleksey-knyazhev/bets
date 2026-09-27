package ru.bets.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import ru.bets.domain.ScoreCalculator;
import ru.bets.exception.RoundValidationException;
import ru.bets.model.CalculationResult;
import ru.bets.model.PlayerRolls;

class ScoreCalculatorTest {

    private final ScoreCalculator scoreCalculator = new ScoreCalculator();

    @Test
    void returnsFirstPartitionWhenItReachesWinningScoreEarlier() {
        CalculationResult result = scoreCalculator.calculate(
                rolls(0, 6, 6, 6, 6, 6, 1, 1, 1, 1, 1),
                rolls(1, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3)
        );

        assertThat(result.winner()).isEqualTo("Партиция 0");
        assertThat(result.winningStep()).isEqualTo(5);
    }

    @Test
    void returnsSecondPartitionWhenItReachesWinningScoreEarlier() {
        CalculationResult result = scoreCalculator.calculate(
                rolls(0, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3),
                rolls(1, 6, 6, 6, 6, 6, 1, 1, 1, 1, 1)
        );

        assertThat(result.winner()).isEqualTo("Партиция 1");
        assertThat(result.winningStep()).isEqualTo(5);
    }

    @Test
    void returnsDrawWhenBothPartitionsReachWinningScoreOnSameStep() {
        CalculationResult result = scoreCalculator.calculate(
                rolls(0, 6, 6, 6, 6, 6, 1, 1, 1, 1, 1),
                rolls(1, 6, 6, 6, 6, 6, 1, 1, 1, 1, 1)
        );

        assertThat(result.winner()).isEqualTo("Ничья");
        assertThat(result.winningStep()).isEqualTo(5);
    }

    @Test
    void returnsDrawWithoutWinningStepWhenNoPartitionReachesWinningScore() {
        CalculationResult result = scoreCalculator.calculate(
                rolls(0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
                rolls(1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2)
        );

        assertThat(result.winner()).isEqualTo("Ничья");
        assertThat(result.winningStep()).isNull();
    }

    @Test
    void rejectsIncompleteRound() {
        assertThatThrownBy(() -> scoreCalculator.calculate(
                new PlayerRolls(0, List.of(1, 2, 3)),
                rolls(1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2)
        ))
                .isInstanceOf(RoundValidationException.class)
                .hasMessage(ScoreCalculator.EMPTY_ROUND_MESSAGE);
    }

    private PlayerRolls rolls(int partition, Integer... rolls) {
        return new PlayerRolls(partition, List.of(rolls));
    }
}
