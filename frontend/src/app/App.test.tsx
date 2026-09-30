import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { App } from "./App";

function jsonResponse(body: unknown, ok = true): Response {
  return {
    ok,
    json: vi.fn().mockResolvedValue(body),
  } as unknown as Response;
}

describe("App round flow", () => {
  const fetchMock = vi.fn<typeof fetch>();

  beforeEach(() => {
    fetchMock.mockReset();
    vi.stubGlobal("fetch", fetchMock);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("generates both partitions, calculates, then resets for the next round", async () => {
    const rolls = [6, 6, 6, 6, 6, 1, 1, 1, 1, 1];
    fetchMock
      .mockResolvedValueOnce(jsonResponse(3))
      .mockResolvedValueOnce(jsonResponse(rolls))
      .mockResolvedValueOnce(jsonResponse(rolls))
      .mockResolvedValueOnce(jsonResponse({ winner: "Партиция 0", winningStep: 5 }))
      .mockResolvedValueOnce(jsonResponse(4));

    render(<App />);

    expect(await screen.findByText("Текущий раунд: #3")).toBeInTheDocument();
    const calculateButton = screen.getByRole("button", { name: /Подсчет результата/ });
    expect(calculateButton).toBeDisabled();

    const panels = screen.getAllByRole("article");
    fireEvent.click(within(panels[0]).getByRole("button", { name: "Сгенерировать 10 бросков" }));
    await waitFor(() => expect(within(panels[0]).getAllByRole("listitem")).toHaveLength(10));
    expect(calculateButton).toBeDisabled();

    fireEvent.click(within(panels[1]).getByRole("button", { name: "Сгенерировать 10 бросков" }));
    await waitFor(() => expect(within(panels[1]).getAllByRole("listitem")).toHaveLength(10));
    expect(calculateButton).toBeEnabled();

    fireEvent.click(calculateButton);
    expect(await screen.findByText("🏆 Победитель: Партиция 0! Набрал 30 очков за 5 ходов")).toBeInTheDocument();
    expect(within(panels[0]).getByText("Бросок #5").closest("li")).toHaveClass("winning-roll");
    expect(within(panels[1]).getAllByRole("listitem").filter((item) => item.classList.contains("winning-roll")))
      .toHaveLength(0);

    fireEvent.click(screen.getByRole("button", { name: /НОВЫЙ РАУНД/ }));

    expect(await screen.findByText("Текущий раунд: #4")).toBeInTheDocument();
    expect(screen.getAllByRole("article").every((panel) => within(panel).queryAllByRole("listitem").length === 0))
      .toBe(true);
    expect(screen.getByText("Сгенерируйте броски (по 10 для каждого)")).toBeInTheDocument();
    expect(calculateButton).toBeDisabled();
  });

  it("shows a server error when generation fails", async () => {
    fetchMock
      .mockResolvedValueOnce(jsonResponse(1))
      .mockResolvedValueOnce(jsonResponse({ message: "Kafka недоступна" }, false));

    render(<App />);
    await screen.findByText("Текущий раунд: #1");
    fireEvent.click(screen.getAllByRole("button", { name: "Сгенерировать 10 бросков" })[0]);

    expect(await screen.findByText("Kafka недоступна")).toBeInTheDocument();
  });
});
