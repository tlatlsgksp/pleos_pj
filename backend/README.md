# Backend

차계부 앱의 서버. NestJS, PostgreSQL(Prisma), Redis(BullMQ).

역할:

- 차계부 기록(주유·정비·지출) 동기화
- 같은 차종 사용자 간 연비·주유 통계와 순위 집계
- 외부 API 프록시(오피넷 주유소·유가). API 키를 앱에 넣지 않기 위함

구조와 규칙은 [아키텍처 문서](../docs/ARCHITECTURE.md#3-백엔드)를 따른다.

## 실행

```sh
sh ../secret/decrypt.sh     # .env 생성. age 키가 없으면 cp .env.example .env
docker compose up -d        # PostgreSQL 17, Redis 7
npm install
npx prisma migrate dev
npm run start:dev
```

## 명령

| 명령 | 내용 |
|---|---|
| `npm run start:dev` | 개발 서버 (watch) |
| `npm run build` | 빌드 → `dist/` |
| `npm run lint` | oxlint |
| `npm test` | 단위 테스트 (vitest) |
| `npm run test:e2e` | e2e 테스트 (`*.e2e-spec.ts`, 아직 없음) |
| `npm run openapi` | 빌드 후 `docs/api/openapi.json` 생성. DB 연결 없이 동작 |
| `npx prisma generate` | 스키마 변경 후 클라이언트 재생성 (`src/generated/prisma`, gitignore) |
| `npx prisma migrate dev` | 마이그레이션 생성·적용 |
| `npx prisma studio` | DB 브라우저 |

## 구조

```
src/
  config/       env 스키마(zod)와 AppConfig. process.env는 여기서만 읽는다
  common/       AppError, 예외 필터, @Public(), ValidationPipe 옵션
  infra/
    prisma/     PrismaService (repository만 주입받는다)
    redis/      ioredis 클라이언트, CacheService, LeaderboardService
    queue/      BullMQ 루트 연결, 큐 이름·job 타입(jobs.ts)
  modules/
    vehicles/   패턴 예시: module → controller → service → repository, dto/, types
  app.setup.ts  버저닝·파이프·필터·Swagger 설정 (main.ts와 openapi.ts가 공유)
  openapi.ts    OpenAPI 문서 생성 진입점
```

Swagger UI는 서버 실행 후 `http://localhost:3000/docs`에서 본다. 규칙은 [아키텍처 문서 §3](../docs/ARCHITECTURE.md#3-백엔드)을 따른다.
