# pleos_pj

현대자동차그룹 차량용 앱 개발 공모전 출품작. Pleos Connect 차량 밀착형 차계부 앱.

## 구조

| 경로 | 내용 |
|---|---|
| `app/` | Pleos Connect(AAOS) 차량 앱. 독립 Gradle 프로젝트 |
| `backend/` | 서버. NestJS, PostgreSQL(Prisma), Redis(BullMQ) |
| `docs/` | 기획·설계·조사 문서 |

## 앱 빌드

Android Studio에서 `app/` 폴더를 연다. CLI는 다음과 같다.

```sh
cd app
./gradlew :automotive:assembleDebug
```

실행 환경은 Pleos Connect Emulator(API 34)다. 설치 방법은 [Pleos 조사 노트 §11](docs/PLEOS_PLATFORM_RESEARCH.md#11-에뮬레이터)에 있다.

## 백엔드 실행

```sh
cd backend
docker compose up -d
cp .env.example .env
npm install
npx prisma migrate dev
npm run start:dev
```

## 문서

- [프로젝트 목표](docs/PROJECT_GOALS.md)
- [아키텍처](docs/ARCHITECTURE.md)
- [앱 설계 원칙](docs/APP_DESIGN_PRINCIPLES.md), [앱 설계 가이드](docs/APP_DESIGN_GUIDE.md), [UI 디자인 가이드](docs/UI_DESIGN_GUIDE.md)
- [Pleos 플랫폼·공모전 조사 노트](docs/PLEOS_PLATFORM_RESEARCH.md)
