package ru.bets.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import ru.bets.exception.RoundValidationException;
import ru.bets.model.CalculationResult;
import ru.bets.service.RoundService;

@WebMvcTest(RoundController.class)
class RoundControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RoundService roundService;

    @Test
    void returnsCurrentRound() throws Exception {
        when(roundService.getCurrentRound()).thenReturn(4);

        mockMvc.perform(get("/api/round"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(4));
    }

    @Test
    void generatesRollsForFirstPlayer() throws Exception {
        when(roundService.generate(1)).thenReturn(List.of(1, 2, 3, 4, 5, 6, 1, 2, 3, 4));

        mockMvc.perform(post("/api/generate/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(10)))
                .andExpect(jsonPath("$[0]").value(1));

        verify(roundService).generate(1);
    }

    @Test
    void generatesRollsForSecondPlayer() throws Exception {
        when(roundService.generate(2)).thenReturn(List.of(6, 5, 4, 3, 2, 1, 6, 5, 4, 3));

        mockMvc.perform(post("/api/generate/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(10)))
                .andExpect(jsonPath("$[0]").value(6));

        verify(roundService).generate(2);
    }

    @Test
    void returnsBadRequestWhenPlayerIdIsInvalid() throws Exception {
        when(roundService.generate(3)).thenThrow(new RoundValidationException("playerId должен быть равен 1 или 2"));

        mockMvc.perform(post("/api/generate/3"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("playerId должен быть равен 1 или 2"));
    }

    @Test
    void returnsCalculationResult() throws Exception {
        when(roundService.calculate()).thenReturn(new CalculationResult("Партиция 1", 5));

        mockMvc.perform(get("/api/calculate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.winner").value("Партиция 1"))
                .andExpect(jsonPath("$.winningStep").value(5));
    }

    @Test
    void returnsBadRequestWhenRoundCannotBeCalculatedYet() throws Exception {
        when(roundService.calculate()).thenThrow(new RoundValidationException("Броски ещё не готовы"));

        mockMvc.perform(get("/api/calculate"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Броски ещё не готовы"));
    }

    @Test
    void startsNextRound() throws Exception {
        when(roundService.nextRound()).thenReturn(5);

        mockMvc.perform(post("/api/next-round"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(5));

        verify(roundService).nextRound();
    }
}
