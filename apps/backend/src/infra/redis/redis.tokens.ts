/** Injection token for the shared ioredis client. Prefer CacheService/LeaderboardService. */
export const REDIS_CLIENT = Symbol('REDIS_CLIENT');
