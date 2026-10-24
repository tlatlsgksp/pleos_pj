import { Inject, Injectable } from '@nestjs/common';
import { Redis } from 'ioredis';
import { REDIS_CLIENT } from './redis.tokens.js';

export interface LeaderboardEntry {
  member: string;
  score: number;
}

/** Sorted-set leaderboard. Higher score ranks first. */
@Injectable()
export class LeaderboardService {
  constructor(@Inject(REDIS_CLIENT) private readonly redis: Redis) {}

  async add(key: string, member: string, score: number): Promise<void> {
    await this.redis.zadd(key, score, member);
  }

  async topN(key: string, n: number): Promise<LeaderboardEntry[]> {
    const flat = await this.redis.zrevrange(key, 0, n - 1, 'WITHSCORES');
    const entries: LeaderboardEntry[] = [];
    for (let i = 0; i < flat.length; i += 2) {
      entries.push({ member: flat[i], score: Number(flat[i + 1]) });
    }
    return entries;
  }

  /** 1-based rank, or null when the member is not on the board. */
  async rankOf(key: string, member: string): Promise<number | null> {
    const rank = await this.redis.zrevrank(key, member);
    return rank === null ? null : rank + 1;
  }
}
