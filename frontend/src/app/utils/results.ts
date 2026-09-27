import type { CalculationResult, Partition } from "../types";

export function formatResult(result: CalculationResult) {
  if (result.winner === "Ничья") {
    return result.winningStep === null
      ? "Ничья. Никто не набрал 30 очков за 10 ходов"
      : `Ничья. Оба участника набрали 30 очков на ходе #${result.winningStep}`;
  }

  return `🏆 Победитель: ${formatWinnerName(result.winner)}! Набрал 30 очков за ${result.winningStep} ходов`;
}

export function getHighlightedStep(result: CalculationResult | null, partition: Partition) {
  if (!result?.winningStep) {
    return null;
  }
  if (result.winner === "Ничья") {
    return result.winningStep;
  }

  return formatWinnerName(result.winner) === `Партиция ${partition}` ? result.winningStep : null;
}

function formatWinnerName(winner: string) {
  return winner.replace(/^Участник \d+ \(Партиция (\d+)\)$/, "Партиция $1");
}
