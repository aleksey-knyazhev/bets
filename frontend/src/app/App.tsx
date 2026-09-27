import { useEffect, useState } from "react";

import "./styles.css";

type CalculationResult = {
  winner: string;
  winningStep: number | null;
};

type PlayerId = 1 | 2;

const initialStatus = "Сгенерируйте броски (по 10 для каждого)";

export function App() {
  const [round, setRound] = useRound();
  const [playerOneRolls, setPlayerOneRolls] = useState<number[]>([]);
  const [playerTwoRolls, setPlayerTwoRolls] = useState<number[]>([]);
  const [loadingPlayer, setLoadingPlayer] = useState<PlayerId | null>(null);
  const [isCalculating, setIsCalculating] = useState(false);
  const [isStartingRound, setIsStartingRound] = useState(false);
  const [calculated, setCalculated] = useState(false);
  const [status, setStatus] = useState(initialStatus);

  const canCalculate =
    playerOneRolls.length === 10 &&
    playerTwoRolls.length === 10 &&
    !calculated &&
    !isCalculating;

  async function generate(playerId: PlayerId) {
    setLoadingPlayer(playerId);
    setStatus(`Генерируются броски для Партиция ${playerId - 1}...`);

    try {
      const response = await fetch(`/api/generate/${playerId}`, { method: "POST" });
      if (!response.ok) {
        throw new Error(await readError(response));
      }

      const rolls = (await response.json()) as number[];
      if (playerId === 1) {
        setPlayerOneRolls(rolls);
      } else {
        setPlayerTwoRolls(rolls);
      }
      setCalculated(false);
      setStatus("Броски загружены. Когда готовы оба участника, запустите подсчет.");
    } catch (error) {
      setStatus(error instanceof Error ? error.message : "Не удалось сгенерировать броски");
    } finally {
      setLoadingPlayer(null);
    }
  }

  async function calculate() {
    setIsCalculating(true);
    setStatus("Идет подсчет результата...");

    try {
      const response = await fetch("/api/calculate");
      if (!response.ok) {
        throw new Error(await readError(response));
      }

      const result = (await response.json()) as CalculationResult;
      setCalculated(true);
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
      const response = await fetch("/api/next-round", { method: "POST" });
      if (!response.ok) {
        throw new Error(await readError(response));
      }

      const next = (await response.json()) as number;
      setRound(next);
      setPlayerOneRolls([]);
      setPlayerTwoRolls([]);
      setCalculated(false);
      setStatus(initialStatus);
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
        <PlayerPanel
          partition={0}
          playerId={1}
          rolls={playerOneRolls}
          isLoading={loadingPlayer === 1}
          onGenerate={generate}
        />
        <PlayerPanel
          partition={1}
          playerId={2}
          rolls={playerTwoRolls}
          isLoading={loadingPlayer === 2}
          onGenerate={generate}
        />
      </section>
    </main>
  );
}

function PlayerPanel({
  playerId,
  partition,
  rolls,
  isLoading,
  onGenerate,
}: {
  playerId: PlayerId;
  partition: number;
  rolls: number[];
  isLoading: boolean;
  onGenerate: (playerId: PlayerId) => void;
}) {
  return (
    <article className="player">
      <h2>Партиция {partition}</h2>
      <button className="generate" disabled={isLoading} onClick={() => onGenerate(playerId)}>
        {isLoading ? "loading..." : "Сгенерировать 10 бросков"}
      </button>
      <ol className="rolls">
        {rolls.map((roll, index) => (
          <li key={`${playerId}-${index}`}>
            <span>Бросок #{index + 1}</span>
            <strong>🎲 {roll}</strong>
          </li>
        ))}
      </ol>
    </article>
  );
}

function useRound() {
  const [round, setRound] = useState<number | null>(null);

  useEffect(() => {
    let isMounted = true;

    fetch("/api/round")
      .then((response) => {
        if (!response.ok) {
          throw new Error("Не удалось загрузить номер раунда");
        }
        return response.json() as Promise<number>;
      })
      .then((value) => {
        if (isMounted) {
          setRound(value);
        }
      })
      .catch(() => {
        if (isMounted) {
          setRound(1);
        }
      });

    return () => {
      isMounted = false;
    };
  }, []);

  return [round, setRound] as const;
}

async function readError(response: Response) {
  try {
    const payload = (await response.json()) as { message?: string };
    return payload.message ?? "Ошибка запроса";
  } catch {
    return "Ошибка запроса";
  }
}

function formatResult(result: CalculationResult) {
  if (result.winner === "Ничья") {
    return result.winningStep === null
      ? "Ничья. Никто не набрал 30 очков за 10 ходов."
      : `Ничья. Оба участника набрали 30 очков на ходе #${result.winningStep}.`;
  }

  return `🏆 Победитель: ${result.winner}! Набрал 30 очков за ${result.winningStep} ходов.`;
}
