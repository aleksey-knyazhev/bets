package ru.bets.service;

import java.util.List;

public interface DiceRollWriter {

    void writeRolls(int round, int partition, List<Integer> rolls);
}
