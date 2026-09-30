import { describe, expect, it } from "vitest";

import { toPartition, toPlayerId } from "./partitions";

describe("partition mapping", () => {
  it.each([
    [1, 0],
    [2, 1],
  ] as const)("maps player %i to partition %i", (playerId, partition) => {
    expect(toPartition(playerId)).toBe(partition);
  });

  it.each([
    [0, 1],
    [1, 2],
  ] as const)("maps partition %i to player %i", (partition, playerId) => {
    expect(toPlayerId(partition)).toBe(playerId);
  });
});
