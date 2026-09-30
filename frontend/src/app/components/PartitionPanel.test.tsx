import { fireEvent, render, screen, within } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { PartitionPanel } from "./PartitionPanel";

describe("PartitionPanel", () => {
  it("renders rolls and highlights the winning roll", () => {
    render(
      <PartitionPanel
        partition={1}
        rolls={[2, 6, 4]}
        isLoading={false}
        highlightedStep={2}
        onGenerate={vi.fn()}
      />,
    );

    const panel = screen.getByRole("article");
    expect(within(panel).getByRole("heading", { name: "Партиция 1" })).toBeInTheDocument();
    expect(within(panel).getAllByRole("listitem")).toHaveLength(3);
    expect(within(panel).getByText("Бросок #2").closest("li")).toHaveClass("winning-roll");
  });

  it("disables generation while loading and sends the mapped player id", () => {
    const onGenerate = vi.fn();
    const { rerender } = render(
      <PartitionPanel
        partition={0}
        rolls={[]}
        isLoading
        highlightedStep={null}
        onGenerate={onGenerate}
      />,
    );

    const loadingButton = screen.getByRole("button", { name: "loading..." });
    expect(loadingButton).toBeDisabled();

    rerender(
      <PartitionPanel
        partition={0}
        rolls={[]}
        isLoading={false}
        highlightedStep={null}
        onGenerate={onGenerate}
      />,
    );
    fireEvent.click(screen.getByRole("button", { name: "Сгенерировать 10 бросков" }));

    expect(onGenerate).toHaveBeenCalledWith(1);
  });
});
