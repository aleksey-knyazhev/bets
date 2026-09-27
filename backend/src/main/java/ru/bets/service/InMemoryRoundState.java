package ru.bets.service;

import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;

@Component
public class InMemoryRoundState implements RoundState {

    private final AtomicInteger currentRound = new AtomicInteger(1);

    @Override
    public int currentRound() {
        return currentRound.get();
    }

    @Override
    public int nextRound() {
        return currentRound.incrementAndGet();
    }
}
