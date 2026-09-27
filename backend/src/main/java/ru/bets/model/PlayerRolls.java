package ru.bets.model;

import java.util.List;

public record PlayerRolls(int partition, List<Integer> rolls) {

    public String displayName() {
        return "Партиция " + partition;
    }
}
