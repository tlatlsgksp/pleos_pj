import { Injectable } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import type { Env } from './env.js';

/** Typed, validated configuration. Inject this instead of reading process.env. */
@Injectable()
export class AppConfig {
  readonly nodeEnv: Env['NODE_ENV'];
  readonly port: number;
  readonly databaseUrl: string;
  readonly redisUrl: string;
  readonly vinHmacSecret: string;

  constructor(config: ConfigService<Env, true>) {
    this.nodeEnv = config.get('NODE_ENV', { infer: true });
    this.port = config.get('PORT', { infer: true });
    this.databaseUrl = config.get('DATABASE_URL', { infer: true });
    this.redisUrl = config.get('REDIS_URL', { infer: true });
    this.vinHmacSecret = config.get('VIN_HMAC_SECRET', { infer: true });
  }

  get isProduction(): boolean {
    return this.nodeEnv === 'production';
  }
}
