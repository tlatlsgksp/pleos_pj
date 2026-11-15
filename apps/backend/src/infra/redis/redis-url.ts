export interface RedisConnectionOptions {
  host: string;
  port: number;
  username?: string;
  password?: string;
  db?: number;
}

/** Splits a redis:// URL into the option object BullMQ and ioredis accept. */
export function parseRedisUrl(url: string): RedisConnectionOptions {
  const parsed = new URL(url);
  const db = parsed.pathname.replace(/^\//, '');
  return {
    host: parsed.hostname,
    port: parsed.port ? Number(parsed.port) : 6379,
    username: parsed.username || undefined,
    password: parsed.password || undefined,
    db: db ? Number(db) : undefined,
  };
}
