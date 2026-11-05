import { describe, expect, it } from 'vitest';
import { validateEnv } from './env.js';

const base = {
  DATABASE_URL: 'postgresql://pleos:pleos@localhost:5432/pleos',
  REDIS_URL: 'redis://localhost:6379',
  VIN_HMAC_SECRET: 'dev-only-change-me-0123456789',
};

describe('validateEnv', () => {
  it('treats empty Naver Maps keys as unset', () => {
    const env = validateEnv({
      ...base,
      NAVER_MAPS_CLIENT_ID: '',
      NAVER_MAPS_CLIENT_SECRET: ' ',
    });
    expect(env.NAVER_MAPS_CLIENT_ID).toBeUndefined();
    expect(env.NAVER_MAPS_CLIENT_SECRET).toBeUndefined();
  });

  it('keeps both Naver Maps keys when set', () => {
    const env = validateEnv({
      ...base,
      NAVER_MAPS_CLIENT_ID: 'id',
      NAVER_MAPS_CLIENT_SECRET: 'secret',
    });
    expect(env.NAVER_MAPS_CLIENT_ID).toBe('id');
    expect(env.NAVER_MAPS_CLIENT_SECRET).toBe('secret');
  });

  it('rejects a Naver Maps client id without its secret', () => {
    expect(() => validateEnv({ ...base, NAVER_MAPS_CLIENT_ID: 'id' })).toThrow(
      /set together/,
    );
  });

  it('rejects a missing database url', () => {
    expect(() => validateEnv({ ...base, DATABASE_URL: undefined })).toThrow(
      /DATABASE_URL/,
    );
  });
});
