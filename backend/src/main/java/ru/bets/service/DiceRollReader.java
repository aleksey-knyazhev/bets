package ru.bets.service;

import ru.bets.model.PlayerRolls;

public interface DiceRollReader {

    PlayerRolls readRoundRolls(int partition, int round);
}
