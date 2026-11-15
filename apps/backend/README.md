# Backend

Moli 서버. NestJS, PostgreSQL(Prisma), Redis(BullMQ).

역할(기획 기준, `docs/FEATURE_DEFINITION.md`):

- 계정·차량 연결(QR), 이웃, 커뮤니티 글·댓글·핀, 도로 제보
- 같은 차종 연비 랭킹 집계
- 그룹 라이딩 위치·워키토키 음성 중계
- 외부 API 프록시: 오피넷 주유소·유가, 네이버 지도(Geocoding). API 키를 앱에 넣지 않기 위함

지금 구현된 모듈은 `fuel-prices`(오피넷 API 형식으로 응답하는 스텁, 외부 호출 없음)와 패턴 예시 `vehicles`다. 스텁 데이터는 `node scripts/generate-opinet-fixtures.mjs`로 다시 만든다.

## 실행

```sh
(cd ../.. && sh secret/decrypt.sh)  # .env 생성. age 키가 없으면 cp .env.example .env
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

Swagger UI는 서버 실행 후 `http://localhost:3000/docs`에서 본다.
