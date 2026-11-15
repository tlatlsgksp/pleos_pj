import {
  INestApplication,
  ValidationPipe,
  VersioningType,
} from '@nestjs/common';
import { DocumentBuilder, OpenAPIObject, SwaggerModule } from '@nestjs/swagger';
import { AppExceptionFilter } from './common/filters/app-exception.filter.js';
import { VALIDATION_PIPE_OPTIONS } from './common/validation.js';

/** Shared by main.ts and the OpenAPI generator so both see the same app. */
export function configureApp(app: INestApplication): void {
  app.enableVersioning({ type: VersioningType.URI, defaultVersion: '1' });
  app.useGlobalPipes(new ValidationPipe(VALIDATION_PIPE_OPTIONS));
  app.useGlobalFilters(new AppExceptionFilter());
  app.enableShutdownHooks();
  // TODO(auth): register the global auth guard here once the auth module
  // exists. Routes opt out with @Public() (src/common/decorators).
}

export function buildOpenApiDocument(app: INestApplication): OpenAPIObject {
  const config = new DocumentBuilder()
    .setTitle('Pleos car log API')
    .setDescription('Backend for the Pleos Connect car expense log app.')
    .setVersion('1')
    .addBearerAuth()
    .build();
  return SwaggerModule.createDocument(app, config);
}
