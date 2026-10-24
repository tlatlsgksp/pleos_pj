# Backend

차계부 앱의 서버. NestJS, PostgreSQL(Prisma), Redis(BullMQ).

역할:

- 차계부 기록(주유·정비·지출) 동기화
- 같은 차종 사용자 간 연비·주유 통계와 순위 집계
- 외부 API 프록시(오피넷 주유소·유가). API 키를 앱에 넣지 않기 위함

구조와 규칙은 [아키텍처 문서](../docs/ARCHITECTURE.md#3-백엔드)를 따른다.

## 실행

```sh
docker compose up -d        # PostgreSQL 17, Redis 7
cp .env.example .env
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
| `npm run test:e2e` | e2e 테스트 |
| `npx prisma migrate dev` | 마이그레이션 생성·적용 |
| `npx prisma studio` | DB 브라우저 |
