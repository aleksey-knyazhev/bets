package ru.bets.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ru.bets.model.CalculationResult;
import ru.bets.service.RoundService;

@RestController
@RequestMapping("/api")
public class RoundController {

    private final RoundService roundService;

    public RoundController(RoundService roundService) {
        this.roundService = roundService;
    }

    @GetMapping("/round")
    public int getRound() {
        return roundService.getCurrentRound();
    }

    @PostMapping("/generate/{playerId}")
    public List<Integer> generate(@PathVariable int playerId) {
        return roundService.generate(playerId);
    }

    @GetMapping("/calculate")
    public CalculationResult calculate() {
        return roundService.calculate();
    }

    @PostMapping("/next-round")
    public int nextRound() {
        return roundService.nextRound();
    }
}
