package ru.bets.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("e2e")
class GameE2ETest {

    private final GamePage gamePage = new GamePage();

    @BeforeAll
    static void configureBrowser() {
        Configuration.baseUrl = System.getProperty(
                "e2e.baseUrl",
                Optional.ofNullable(System.getenv("E2E_BASE_URL")).orElse("http://localhost:5173")
        );
        Configuration.browser = "chrome";
        Configuration.headless = true;
        Configuration.browserSize = "1440x1000";
        Configuration.timeout = 10_000;
        Optional.ofNullable(System.getenv("SELENIDE_REMOTE"))
                .filter(remote -> !remote.isBlank())
                .ifPresent(remote -> Configuration.remote = remote);
    }

    @AfterEach
    void closeBrowser() {
        Selenide.closeWebDriver();
    }

    @Test
    void showsRoundAndDisablesCalculationBeforeRollsAreGenerated() {
        gamePage.open();

        assertThat(gamePage.currentRound()).isPositive();
        gamePage.calculateIsDisabled();
    }

    @Test
    void generatesBothPartitionsAndShowsAConsistentCalculationResult() {
        gamePage.open();
        gamePage.generateRolls(0);
        gamePage.generateRolls(1);

        assertThat(gamePage.rollValues(0)).hasSize(10).allSatisfy(roll -> assertThat(roll).isBetween(1, 6));
        assertThat(gamePage.rollValues(1)).hasSize(10).allSatisfy(roll -> assertThat(roll).isBetween(1, 6));

        gamePage.calculateWhenEnabled();

        assertTrue(gamePage.resultText().contains("Победитель:") || gamePage.resultText().contains("Ничья."));
        gamePage.assertHighlightsMatchResult();
    }

    @Test
    void nextRoundClearsGeneratedRollsAndCalculationResult() {
        gamePage.open();
        int previousRound = gamePage.currentRound();
        gamePage.generateRolls(0);
        gamePage.generateRolls(1);
        gamePage.calculateWhenEnabled();

        gamePage.startNextRound();

        gamePage.awaitRound(previousRound + 1);
        gamePage.awaitEmptyPanelsAndInitialStatus();
    }
}
