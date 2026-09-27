import type { CalculationResult, PlayerId } from "../types";

export async function getRound() {
  const response = await fetch("/api/round");
  if (!response.ok) {
    throw new Error("Не удалось загрузить номер раунда");
  }
  return response.json() as Promise<number>;
}

export async function generateRolls(playerId: PlayerId) {
  const response = await fetch(`/api/generate/${playerId}`, { method: "POST" });
  if (!response.ok) {
    throw new Error(await readError(response));
  }
  return response.json() as Promise<number[]>;
}

export async function calculateRound() {
  const response = await fetch("/api/calculate");
  if (!response.ok) {
    throw new Error(await readError(response));
  }
  return response.json() as Promise<CalculationResult>;
}

export async function startNextRound() {
  const response = await fetch("/api/next-round", { method: "POST" });
  if (!response.ok) {
    throw new Error(await readError(response));
  }
  return response.json() as Promise<number>;
}

async function readError(response: Response) {
  try {
    const payload = (await response.json()) as { message?: string };
    return payload.message ?? "Ошибка запроса";
  } catch {
    return "Ошибка запроса";
  }
}
