# pleos_pj

현대자동차그룹 차량용 앱 개발 공모전 출품작. Pleos Connect 차량 밀착형 차계부 앱.

## 구조

| 경로 | 내용 |
|---|---|
| `app/` | Pleos Connect(AAOS) 차량 앱. 독립 Gradle 프로젝트 |
| `backend/` | 서버. NestJS, PostgreSQL(Prisma), Redis(BullMQ) |
| `docs/` | 기획·설계·조사 문서 |
| `archive/` | 보류한 기능의 시제품. 다시 꺼낼 때 각 폴더 README부터 본다 ([차량 스킨](archive/skin-prototype/README.md)) |

## 앱 빌드

Android Studio에서 `app/` 폴더를 연다. CLI는 다음과 같다.

```sh
cd app
./gradlew :automotive:assembleDebug
```

실행 환경은 Pleos Connect Emulator(API 34)다. 설치 방법은 [Pleos 조사 노트 §11](docs/PLEOS_PLATFORM_RESEARCH.md#11-에뮬레이터)에 있다.

## 백엔드 실행

```sh
sh secret/decrypt.sh            # backend/.env 생성. age 키가 없으면 cp backend/.env.example backend/.env
cd backend
docker compose up -d
npm install
npx prisma migrate dev
npm run start:dev
```

## 비밀 값

API 키 같은 비밀 값은 `secret/`에 [sops](https://github.com/getsops/sops)와 age로 암호화해 커밋한다. 환경은 하나다(dev/prod를 나누지 않는다).

| 암호화 파일 (커밋) | 평문이 풀리는 위치 (gitignore) | 내용 |
|---|---|---|
| `secret/backend.env` | `backend/.env` | DB·Redis URL, VIN HMAC 키, 네이버 Maps 클라이언트 ID·Secret |
| `secret/app.env` | `app/secrets.properties` | Pleos Playground 인증 정보, 네이버 Maps 클라이언트 ID |

```sh
sh secret/decrypt.sh    # 풀기. age 개인키가 있어야 한다
sh secret/encrypt.sh    # 평문을 고친 뒤 다시 잠그기. 내용이 같으면 파일을 건드리지 않는다
```

PowerShell에서는 스크립트 대신 sops를 직접 부른다. `>` 리다이렉션은 UTF-16으로 저장되므로 쓰지 않는다.

```sh
sops -d --output backend/.env secret/backend.env
sops -d --output app/secrets.properties secret/app.env
```

age 개인키는 sops 기본 경로에 둔다: Windows `%APPDATA%\sops\age\keys.txt`, macOS `~/Library/Application Support/sops/age/keys.txt`, Linux `~/.config/sops/age/keys.txt`. 새 사람에게 접근을 주려면 그 사람이 `age-keygen`으로 만든 공개키를 `.sops.yaml`에 추가하고, 기존 키 보유자가 `sops updatekeys secret/backend.env secret/app.env`를 실행한다.

네이버 Maps 클라이언트 ID는 앱 패키지 이름에 묶여 있어 앱에 넣어도 되지만, Secret은 Geocoding을 호출하는 백엔드에만 둔다.

## 문서

- [프로젝트 목표](docs/PROJECT_GOALS.md)
- [아키텍처](docs/ARCHITECTURE.md)
- [앱 설계 원칙](docs/APP_DESIGN_PRINCIPLES.md), [앱 설계 가이드](docs/APP_DESIGN_GUIDE.md), [UI 디자인 가이드](docs/UI_DESIGN_GUIDE.md)
- [Pleos 플랫폼·공모전 조사 노트](docs/PLEOS_PLATFORM_RESEARCH.md)
- [기획 기반 정리](docs/PLANNING_FOUNDATION.md), [기획서 초안](docs/PROPOSAL_DRAFT.md), [Pleos API 개방 제안](docs/PLEOS_API_PROPOSAL.md)
