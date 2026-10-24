// Writes the OpenAPI document to docs/api/openapi.json without starting
// the HTTP server or connecting to the database. Run with `npm run openapi`.
import { mkdir, writeFile } from 'node:fs/promises';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { NestFactory } from '@nestjs/core';
import { AppModule } from './app.module.js';
import { buildOpenApiDocument, configureApp } from './app.setup.js';

const outputPath = resolve(
  dirname(fileURLToPath(import.meta.url)),
  '../../docs/api/openapi.json',
);

// NestFactory.create instantiates providers but does not run lifecycle hooks
// (no app.init()), so PrismaService never connects.
const app = await NestFactory.create(AppModule, { logger: ['error', 'warn'] });
configureApp(app);
const document = buildOpenApiDocument(app);

await mkdir(dirname(outputPath), { recursive: true });
await writeFile(outputPath, `${JSON.stringify(document, null, 2)}\n`);
console.log(`Wrote ${outputPath}`);

await app.close();
