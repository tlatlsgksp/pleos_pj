# pleos_pj

현대자동차그룹 차량용 앱 개발 공모전 작업 저장소. Pleos Connect(AAOS) 차량 앱과 서버의 스캐폴드, 플랫폼 조사 자료를 담는다.

| 경로 | 내용 |
|---|---|
| `apps/android/` | Pleos Connect 차량 앱 스캐폴드 (Kotlin, Compose). 독립 Gradle 프로젝트 |
| `apps/backend/` | 서버 스캐폴드 (NestJS, PostgreSQL/Prisma, Redis/BullMQ). [실행 방법](apps/backend/README.md) |
| `docs/` | [Pleos 플랫폼·공모전 조사 노트](docs/PLEOS_PLATFORM_RESEARCH.md), [앱 설계 원칙](docs/APP_DESIGN_PRINCIPLES.md), [앱 설계 가이드](docs/APP_DESIGN_GUIDE.md), [UI 디자인 가이드](docs/UI_DESIGN_GUIDE.md) |
| `secret/` | sops·age로 암호화한 비밀 값. `sh secret/decrypt.sh`로 푼다 |
