package ru.bets.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class InMemoryRoundStateTest {

    @Test
    void startsAtRoundOneAndIncrementsForEachNextRound() {
        InMemoryRoundState state = new InMemoryRoundState();

        assertThat(state.currentRound()).isEqualTo(1);
        assertThat(state.nextRound()).isEqualTo(2);
        assertThat(state.currentRound()).isEqualTo(2);
        assertThat(state.nextRound()).isEqualTo(3);
    }
}
