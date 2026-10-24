import type { ValidationPipeOptions } from '@nestjs/common';

/** Global request validation: unknown fields are rejected, types are coerced. */
export const VALIDATION_PIPE_OPTIONS: ValidationPipeOptions = {
  whitelist: true,
  forbidNonWhitelisted: true,
  transform: true,
};
