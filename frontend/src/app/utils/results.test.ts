import { describe, expect, it } from "vitest";

import { formatResult, getHighlightedStep } from "./results";
import type { CalculationResult } from "../types";

describe("result presentation", () => {
  it("formats a partition winner", () => {
    expect(formatResult({ winner: "Партиция 0", winningStep: 5 })).toBe(
      "🏆 Победитель: Партиция 0! Набрал 30 очков за 5 ходов",
    );
  });

  it("formats the backend participant name", () => {
    expect(formatResult({ winner: "Участник 1 (Партиция 1)", winningStep: 6 })).toBe(
      "🏆 Победитель: Партиция 1! Набрал 30 очков за 6 ходов",
    );
  });

  it("formats draws with and without a winning step", () => {
    expect(formatResult({ winner: "Ничья", winningStep: 4 })).toBe(
      "Ничья. Оба участника набрали 30 очков на ходе #4",
    );
    expect(formatResult({ winner: "Ничья", winningStep: null })).toBe(
      "Ничья. Никто не набрал 30 очков за 10 ходов",
    );
  });

  it("highlights only the winner's step or both steps for a draw", () => {
    const winner: CalculationResult = { winner: "Партиция 1", winningStep: 7 };
    const draw: CalculationResult = { winner: "Ничья", winningStep: 5 };
    const noWinner: CalculationResult = { winner: "Ничья", winningStep: null };

    expect(getHighlightedStep(winner, 0)).toBeNull();
    expect(getHighlightedStep(winner, 1)).toBe(7);
    expect(getHighlightedStep(draw, 0)).toBe(5);
    expect(getHighlightedStep(draw, 1)).toBe(5);
    expect(getHighlightedStep(noWinner, 0)).toBeNull();
    expect(getHighlightedStep(null, 1)).toBeNull();
  });
});
