import {
  ArgumentsHost,
  Catch,
  ExceptionFilter,
  HttpException,
  HttpStatus,
  Logger,
} from '@nestjs/common';
import type { Response } from 'express';
import { AppError } from '../errors/app-error.js';

interface ErrorBody {
  code: string;
  message: string;
}

/** Maps every thrown error to `{ code, message }`. Never leaks stacks. */
@Catch()
export class AppExceptionFilter implements ExceptionFilter {
  private readonly logger = new Logger(AppExceptionFilter.name);

  catch(exception: unknown, host: ArgumentsHost): void {
    const response = host.switchToHttp().getResponse<Response>();
    const { status, body } = this.toResponse(exception);
    response.status(status).json(body);
  }

  private toResponse(exception: unknown): { status: number; body: ErrorBody } {
    if (exception instanceof AppError) {
      return {
        status: exception.status,
        body: { code: exception.code, message: exception.message },
      };
    }

    if (exception instanceof HttpException) {
      const status = exception.getStatus();
      return {
        status,
        body: {
          code: HttpStatus[status] ?? 'HTTP_ERROR',
          message: this.httpMessage(exception),
        },
      };
    }

    this.logger.error(
      'Unhandled exception',
      exception instanceof Error ? exception.stack : String(exception),
    );
    return {
      status: 500,
      body: { code: 'INTERNAL_ERROR', message: 'Internal server error' },
    };
  }

  private httpMessage(exception: HttpException): string {
    const body = exception.getResponse();
    if (typeof body === 'string') return body;
    const message = (body as { message?: string | string[] }).message;
    if (Array.isArray(message)) return message.join('; ');
    return message ?? exception.message;
  }
}
