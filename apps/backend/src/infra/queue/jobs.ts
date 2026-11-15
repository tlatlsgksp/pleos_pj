/**
 * The single place that names queues and types their payloads.
 * Modules import from here when they register a queue or a processor.
 */
export const QUEUES = {
  example: 'example',
} as const;

export type QueueName = (typeof QUEUES)[keyof typeof QUEUES];

/** Example payload; replace with the first real job. */
export interface ExampleJob {
  id: string;
}

export interface JobPayloads {
  [QUEUES.example]: ExampleJob;
}
