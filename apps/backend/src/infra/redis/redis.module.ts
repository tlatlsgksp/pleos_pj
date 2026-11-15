import { Global, Inject, Module, OnApplicationShutdown } from '@nestjs/common';
import { Redis } from 'ioredis';
import { AppConfig } from '../../config/app.config.js';
import { CacheService } from './cache.service.js';
import { LeaderboardService } from './leaderboard.service.js';
import { REDIS_CLIENT } from './redis.tokens.js';

@Global()
@Module({
  providers: [
    {
      provide: REDIS_CLIENT,
      inject: [AppConfig],
      // lazyConnect: the first command opens the connection, so booting the
      // app (and generating the OpenAPI document) does not need Redis.
      useFactory: (config: AppConfig) =>
        new Redis(config.redisUrl, { lazyConnect: true }),
    },
    CacheService,
    LeaderboardService,
  ],
  exports: [REDIS_CLIENT, CacheService, LeaderboardService],
})
export class RedisModule implements OnApplicationShutdown {
  constructor(@Inject(REDIS_CLIENT) private readonly redis: Redis) {}

  async onApplicationShutdown(): Promise<void> {
    if (this.redis.status === 'wait' || this.redis.status === 'end') {
      this.redis.disconnect();
      return;
    }
    await this.redis.quit();
  }
}
