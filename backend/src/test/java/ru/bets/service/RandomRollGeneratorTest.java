package ru.bets.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class RandomRollGeneratorTest {

    private final RandomRollGenerator generator = new RandomRollGenerator();

    @Test
    void generatesRequestedNumberOfDiceValuesInRange() {
        List<Integer> rolls = generator.generate(10);

        assertThat(rolls).hasSize(10).allSatisfy(roll -> assertThat(roll).isBetween(1, 6));
    }

    @Test
    void returnsEmptyListWhenZeroRollsAreRequested() {
        assertThat(generator.generate(0)).isEmpty();
    }
}
