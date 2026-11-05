/**
 * Base class for errors the API returns on purpose. The global filter turns
 * these into `{ code, message }` with the given HTTP status.
 */
export class AppError extends Error {
  constructor(
    readonly code: string,
    message: string,
    readonly status: number = 500,
  ) {
    super(message);
    this.name = new.target.name;
  }
}

export class NotFoundError extends AppError {
  constructor(code: string, message: string) {
    super(code, message, 404);
  }
}

export class ValidationError extends AppError {
  constructor(code: string, message: string) {
    super(code, message, 400);
  }
}
