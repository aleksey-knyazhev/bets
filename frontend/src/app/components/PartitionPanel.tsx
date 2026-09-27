import type { Partition, PlayerId } from "../types";
import { toPlayerId } from "../utils/partitions";

type PartitionPanelProps = {
  partition: Partition;
  rolls: number[];
  isLoading: boolean;
  highlightedStep: number | null;
  onGenerate: (playerId: PlayerId) => void;
};

export function PartitionPanel({
  partition,
  rolls,
  isLoading,
  highlightedStep,
  onGenerate,
}: PartitionPanelProps) {
  const playerId = toPlayerId(partition);

  return (
    <article className="player">
      <h2>Партиция {partition}</h2>
      <button className="generate" disabled={isLoading} onClick={() => onGenerate(playerId)}>
        {isLoading ? "loading..." : "Сгенерировать 10 бросков"}
      </button>
      <ol className="rolls">
        {rolls.map((roll, index) => (
          <li
            className={highlightedStep === index + 1 ? "winning-roll" : undefined}
            key={`${partition}-${index}`}
          >
            <span>Бросок #{index + 1}</span>
            <strong>🎲 {roll}</strong>
          </li>
        ))}
      </ol>
    </article>
  );
}
