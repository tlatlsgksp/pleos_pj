import { BullModule } from '@nestjs/bullmq';
import { Module } from '@nestjs/common';
import { AppConfig } from '../../config/app.config.js';
import { parseRedisUrl } from '../redis/redis-url.js';

/**
 * BullMQ root connection. Feature modules register their own queues with
 * `BullModule.registerQueue({ name: QUEUES.x })` and handle jobs in a
 * `*.processor.ts`.
 */
@Module({
  imports: [
    BullModule.forRootAsync({
      inject: [AppConfig],
      useFactory: (config: AppConfig) => ({
        connection: {
          ...parseRedisUrl(config.redisUrl),
          // Required by BullMQ workers so blocking commands are not retried.
          maxRetriesPerRequest: null,
        },
      }),
    }),
  ],
})
export class QueueModule {}
