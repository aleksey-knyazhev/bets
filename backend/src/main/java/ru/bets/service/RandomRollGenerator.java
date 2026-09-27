package ru.bets.service;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Component;

import ru.bets.domain.ScoreCalculator;

@Component
public class RandomRollGenerator implements RollGenerator {

    @Override
    public List<Integer> generate(int count) {
        return ThreadLocalRandom.current()
                .ints(count, 1, 7)
                .boxed()
                .toList();
    }
}
