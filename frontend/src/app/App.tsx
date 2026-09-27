import { useState } from "react";

import { calculateRound, generateRolls, startNextRound } from "./api/roundApi";
import { PartitionPanel } from "./components/PartitionPanel";
import { INITIAL_STATUS, ROLLS_PER_PARTITION } from "./constants";
import { useRound } from "./hooks/useRound";
import type { CalculationResult, Partition, PlayerId } from "./types";
import { toPartition } from "./utils/partitions";
import { formatResult, getHighlightedStep } from "./utils/results";

import "./styles.css";

export function App() {
  const [round, setRound] = useRound();
  const [partitionZeroRolls, setPartitionZeroRolls] = useState<number[]>([]);
  const [partitionOneRolls, setPartitionOneRolls] = useState<number[]>([]);
  const [loadingPartition, setLoadingPartition] = useState<Partition | null>(null);
  const [isCalculating, setIsCalculating] = useState(false);
  const [isStartingRound, setIsStartingRound] = useState(false);
  const [calculated, setCalculated] = useState(false);
  const [calculationResult, setCalculationResult] = useState<CalculationResult | null>(null);
  const [status, setStatus] = useState(INITIAL_STATUS);

  const canCalculate =
    partitionZeroRolls.length === ROLLS_PER_PARTITION &&
    partitionOneRolls.length === ROLLS_PER_PARTITION &&
    !calculated &&
    !isCalculating;

  async function generate(playerId: PlayerId) {
    const partition = toPartition(playerId);
    setLoadingPartition(partition);
    setStatus(`Генерируются броски для Партиция ${partition}...`);

    try {
      const rolls = await generateRolls(playerId);
      if (partition === 0) {
        setPartitionZeroRolls(rolls);
      } else {
        setPartitionOneRolls(rolls);
      }
      setCalculated(false);
      setCalculationResult(null);
      setStatus("Броски загружены. Запустите подсчет");
    } catch (error) {
      setStatus(error instanceof Error ? error.message : "Не удалось сгенерировать броски");
    } finally {
      setLoadingPartition(null);
    }
  }

  async function calculate() {
    setIsCalculating(true);
    setStatus("Идет подсчет результата...");

    try {
      const result = await calculateRound();
      setCalculated(true);
      setCalculationResult(result);
      setStatus(formatResult(result));
    } catch (error) {
      setStatus(error instanceof Error ? error.message : "Не удалось рассчитать результат");
    } finally {
      setIsCalculating(false);
    }
  }

  async function nextRound() {
    setIsStartingRound(true);
    setStatus("Запускается новый раунд...");

    try {
      const next = await startNextRound();
      setRound(next);
      setPartitionZeroRolls([]);
      setPartitionOneRolls([]);
      setCalculated(false);
      setCalculationResult(null);
      setStatus(INITIAL_STATUS);
    } catch (error) {
      setStatus(error instanceof Error ? error.message : "Не удалось начать новый раунд");
    } finally {
      setIsStartingRound(false);
    }
  }

  return (
    <main className="shell">
      <header className="header">
        <div className="round">Текущий раунд: #{round ?? "..."}</div>
        <div className="scoreboard" aria-live="polite">
          {status}
        </div>
        <div className="controls">
          <button className="primary" disabled={!canCalculate} onClick={calculate}>
            {isCalculating ? "loading..." : "🎲 Подсчет результата"}
          </button>
          <button disabled={isStartingRound} onClick={nextRound}>
            {isStartingRound ? "loading..." : "🔄 НОВЫЙ РАУНД"}
          </button>
        </div>
      </header>

      <section className="field">
        <PartitionPanel
          partition={0}
          rolls={partitionZeroRolls}
          isLoading={loadingPartition === 0}
          highlightedStep={getHighlightedStep(calculationResult, 0)}
          onGenerate={generate}
        />
        <PartitionPanel
          partition={1}
          rolls={partitionOneRolls}
          isLoading={loadingPartition === 1}
          highlightedStep={getHighlightedStep(calculationResult, 1)}
          onGenerate={generate}
        />
      </section>
    </main>
  );
}
