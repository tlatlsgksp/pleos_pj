/**
 * The single place that names queues and types their payloads.
 * Modules import from here when they register a queue or a processor.
 */
export const QUEUES = {
  statsAggregate: 'stats-aggregate',
  opinetRefresh: 'opinet-refresh',
} as const;

export type QueueName = (typeof QUEUES)[keyof typeof QUEUES];

/** Recompute same-model statistics and the leaderboard for one month. */
export interface StatsAggregateJob {
  modelKey: string;
  /** yyyy-MM */
  month: string;
}

/** Refresh cached fuel prices for one grid cell (rounded lat/lng). */
export interface OpinetRefreshJob {
  gridKey: string;
}

export interface JobPayloads {
  [QUEUES.statsAggregate]: StatsAggregateJob;
  [QUEUES.opinetRefresh]: OpinetRefreshJob;
}
