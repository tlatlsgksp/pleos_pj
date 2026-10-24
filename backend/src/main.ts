import { NestFactory } from '@nestjs/core';
import { SwaggerModule } from '@nestjs/swagger';
import { AppModule } from './app.module.js';
import { buildOpenApiDocument, configureApp } from './app.setup.js';
import { AppConfig } from './config/app.config.js';

async function bootstrap(): Promise<void> {
  const app = await NestFactory.create(AppModule);
  configureApp(app);
  SwaggerModule.setup('docs', app, buildOpenApiDocument(app));
  await app.listen(app.get(AppConfig).port);
}

await bootstrap();
