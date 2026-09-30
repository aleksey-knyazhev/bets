package ru.bets.e2e;

import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.matchText;
import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

class GamePage {

    private static final Pattern ROUND_NUMBER = Pattern.compile("#(\\d+)");

    void open() {
        com.codeborne.selenide.Selenide.open("/");
        roundLabel().shouldHave(matchText("Текущий раунд: #\\d+"));
    }

    int currentRound() {
        Matcher matcher = ROUND_NUMBER.matcher(roundLabel().getText());
        if (!matcher.find()) {
            throw new AssertionError("Round number is not displayed: " + roundLabel().getText());
        }
        return Integer.parseInt(matcher.group(1));
    }

    void generateRolls(int partition) {
        panel(partition).$("button.generate")
                .shouldHave(text("Сгенерировать 10 бросков"))
                .click();
        rollItems(partition).shouldHave(size(10));
    }

    List<Integer> rollValues(int partition) {
        return panel(partition).$$(".rolls li strong").texts().stream()
                .map(text -> Integer.parseInt(text.replaceAll("[^0-9]", "")))
                .toList();
    }

    void calculateWhenEnabled() {
        calculateButton().shouldBe(enabled).click();
        scoreboard().shouldHave(matchText("^(🏆 Победитель:|Ничья\\.).*"));
    }

    void calculateIsDisabled() {
        calculateButton().shouldBe(disabled);
    }

    void startNextRound() {
        $$("button").findBy(text("НОВЫЙ РАУНД")).click();
    }

    void awaitRound(int round) {
        roundLabel().shouldHave(text("#" + round));
    }

    void awaitEmptyPanelsAndInitialStatus() {
        rollItems(0).shouldHave(size(0));
        rollItems(1).shouldHave(size(0));
        scoreboard().shouldHave(text("Сгенерируйте броски (по 10 для каждого)"));
        calculateIsDisabled();
    }

    String resultText() {
        return scoreboard().getText();
    }

    void assertHighlightsMatchResult() {
        String result = resultText();
        Matcher winnerMatcher = Pattern.compile("Победитель: Партиция (\\d+)").matcher(result);
        Matcher winnerStepMatcher = Pattern.compile("за (\\d+) ходов").matcher(result);

        if (winnerMatcher.find()) {
            int winnerPartition = Integer.parseInt(winnerMatcher.group(1));
            if (!winnerStepMatcher.find()) {
                throw new AssertionError("Winner's winning step is missing: " + result);
            }
            int winningStep = Integer.parseInt(winnerStepMatcher.group(1));
            assertHighlightedStep(winnerPartition, winningStep);
            assertNoHighlights(1 - winnerPartition);
            return;
        }

        Matcher drawStepMatcher = Pattern.compile("на ходе #(\\d+)").matcher(result);
        if (drawStepMatcher.find()) {
            int winningStep = Integer.parseInt(drawStepMatcher.group(1));
            assertHighlightedStep(0, winningStep);
            assertHighlightedStep(1, winningStep);
        } else {
            assertNoHighlights(0);
            assertNoHighlights(1);
        }
    }

    private void assertHighlightedStep(int partition, int step) {
        rollItems(partition).get(step - 1).shouldHave(cssClass("winning-roll"));
        highlightedItems(partition).shouldHave(size(1));
    }

    private void assertNoHighlights(int partition) {
        highlightedItems(partition).shouldHave(size(0));
    }

    private ElementsCollection rollItems(int partition) {
        return panel(partition).$$(".rolls li");
    }

    private ElementsCollection highlightedItems(int partition) {
        return rollItems(partition).filterBy(cssClass("winning-roll"));
    }

    private SelenideElement panel(int partition) {
        return $$(".player").findBy(text("Партиция " + partition));
    }

    private SelenideElement roundLabel() {
        return $(".round");
    }

    private SelenideElement scoreboard() {
        return $(".scoreboard");
    }

    private SelenideElement calculateButton() {
        return $("button.primary");
    }
}
