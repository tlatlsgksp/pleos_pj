import { z } from 'zod';

/** Empty strings count as unset, so a blank line in .env disables the feature. */
const optionalSecret = z
  .string()
  .trim()
  .transform((value) => (value === '' ? undefined : value))
  .optional();

// The only place that knows the shape of process.env. Everything else
// injects AppConfig.
export const envSchema = z
  .object({
    NODE_ENV: z
      .enum(['development', 'test', 'production'])
      .default('development'),
    PORT: z.coerce.number().int().positive().default(3000),
    DATABASE_URL: z.url(),
    REDIS_URL: z.url(),
    VIN_HMAC_SECRET: z.string().min(16),
    // Naver Cloud Maps REST (Geocoding, Reverse Geocoding). Set both or neither.
    NAVER_MAPS_CLIENT_ID: optionalSecret,
    NAVER_MAPS_CLIENT_SECRET: optionalSecret,
  })
  .refine(
    (env) =>
      Boolean(env.NAVER_MAPS_CLIENT_ID) ===
      Boolean(env.NAVER_MAPS_CLIENT_SECRET),
    {
      error:
        'NAVER_MAPS_CLIENT_ID and NAVER_MAPS_CLIENT_SECRET must be set together',
      path: ['NAVER_MAPS_CLIENT_SECRET'],
    },
  );

export type Env = z.infer<typeof envSchema>;

export function validateEnv(raw: Record<string, unknown>): Env {
  const result = envSchema.safeParse(raw);
  if (!result.success) {
    throw new Error(`Invalid environment:\n${z.prettifyError(result.error)}`);
  }
  return result.data;
}
