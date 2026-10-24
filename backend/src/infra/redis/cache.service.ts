import { Inject, Injectable } from '@nestjs/common';
import { Redis } from 'ioredis';
import { REDIS_CLIENT } from './redis.tokens.js';

/** JSON cache with TTL. Used for external API responses and computed stats. */
@Injectable()
export class CacheService {
  constructor(@Inject(REDIS_CLIENT) private readonly redis: Redis) {}

  async getJson<T>(key: string): Promise<T | null> {
    const raw = await this.redis.get(key);
    return raw === null ? null : (JSON.parse(raw) as T);
  }

  async setJson(key: string, value: unknown, ttlSeconds: number): Promise<void> {
    await this.redis.set(key, JSON.stringify(value), 'EX', ttlSeconds);
  }

  async del(key: string): Promise<void> {
    await this.redis.del(key);
  }
}
