import { SetMetadata } from '@nestjs/common';

export const IS_PUBLIC_KEY = 'isPublic';

/** Opts a route out of the global auth guard. Every other route requires a token. */
export const Public = () => SetMetadata(IS_PUBLIC_KEY, true);
