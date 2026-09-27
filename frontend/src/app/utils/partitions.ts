import type { PlayerId, Partition } from "../types";

export function toPartition(playerId: PlayerId): Partition {
  return (playerId - 1) as Partition;
}

export function toPlayerId(partition: Partition): PlayerId {
  return (partition + 1) as PlayerId;
}
