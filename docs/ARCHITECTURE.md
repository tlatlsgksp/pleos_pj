# 아키텍처

앱과 백엔드의 구조, 계층, 캡슐화 규칙을 정한다. 구현은 이 문서를 따르고, 바꿀 때는 문서를 먼저 고친다.

결정일: 2026-10-24. 근거가 되는 플랫폼 제약은 [Pleos 조사 노트](PLEOS_PLATFORM_RESEARCH.md)에 있다.

## 0. 결정 요약

| 영역 | 결정 |
|---|---|
| 앱 언어·UI | Kotlin, Jetpack Compose |
| 앱 패턴 | **MVI** (화면마다 불변 `State`, sealed `Event`, 일회성 `Effect`. 단방향 흐름) |
| 앱 DI | Hilt (KSP) |
| 앱 비동기 | Coroutines, Flow |
| 앱 저장소 | Room(기록, 오프라인 우선), DataStore(설정·동의) |
| 앱 네트워크 | Retrofit, OkHttp, kotlinx.serialization |
| 앱 모듈 | `automotive`(앱), `core/domain`(순수 Kotlin), `core/data`, `core/platform` |
| 백엔드 | NestJS 12 (TypeScript strict, ESM) |
| DB | PostgreSQL 17, Prisma 7 (`@prisma/adapter-pg`) |
| 캐시·큐 | Redis 7. 캐시와 리더보드는 ioredis, 작업 큐는 BullMQ |
| 백엔드 구조 | **모듈형 모놀리스 + 계층** (controller → service → repository) |
| API 계약 | OpenAPI. `@nestjs/swagger`로 생성해 `docs/api/openapi.json`에 커밋 |
| 외부 API | 앱은 외부 API(오피넷 등)를 직접 호출하지 않는다. 전부 백엔드를 거친다 |
| Pleos SDK | 앱(AAOS)에서만 호출한다. 백엔드는 SDK를 모른다 |

## 1. 전체 구성

```
┌──────────────── Pleos Connect 차량 (AAOS) ────────────────┐
│  app (automotive)                                          │
│    ui ──▶ domain ◀── data ──▶ Room / Retrofit               │
│                   ◀── platform ──▶ Pleos SDK (Vehicle, Navi, STT, TTS, FusedLocation)
│                                ──▶ AAOS Car API (속도, 시동, 잔여거리)
│                                ──▶ Fake 게이트웨이 (데모 시나리오)
└──────────────────────────┬─────────────────────────────────┘
                           │ HTTPS (OpenAPI, Bearer token)
┌──────────────────────────▼─────────────────────────────────┐
│  backend (NestJS)                                           │
│    modules: auth, vehicles, fuel-logs, maintenance, stats, poi
│    infra:   Prisma ──▶ PostgreSQL                           │
│             Redis  ──▶ 캐시, 리더보드(ZSET), BullMQ 큐        │
│    외부:    오피넷 API (주유소·유가)                           │
└────────────────────────────────────────────────────────────┘
```

경계 원칙:

- 차량 데이터는 앱 안에서만 SDK로 읽고, 서버로는 **가명화된 요약값**만 보낸다.
- 서버 호출은 사용자가 동의한 뒤에만 한다. 동의 전에는 앱이 로컬에서만 동작한다.
- 앱은 네트워크가 끊겨도 핵심 기능(기록, 알림)이 되게 Room을 진실 공급원(source of truth)으로 둔다.

## 2. 앱

### 2.1 계층

| 계층 | 모듈 | 내용 | 의존 가능 대상 |
|---|---|---|---|
| ui | `automotive` | 기능별 `Screen`, `ViewModel`, `Contract`(State·Event·Effect), 내비게이션, 테마 | domain |
| domain | `core/domain` | 도메인 모델, use case, **게이트웨이·저장소 인터페이스**. 순수 Kotlin(JVM), Android 의존 없음 | 없음 |
| data | `core/data` | 저장소 구현, Room, Retrofit, DTO·엔티티 매퍼, 동기화 | domain |
| platform | `core/platform` | Pleos SDK·AAOS·Android API 게이트웨이 구현, Fake 구현, 데모 시나리오 | domain |

의존 방향은 `ui → domain ← data, platform`이다. data와 platform은 서로 모른다. ui는 data·platform의 구현 클래스를 모르고 Hilt 바인딩으로만 연결된다.

처음에는 이 4개 모듈로 시작한다. 기능별 모듈 분리는 빌드 시간이 문제가 될 때만 한다.

### 2.2 MVI 계약

화면(feature)마다 `Contract` 파일 하나에 세 타입을 정의한다.

```kotlin
// ui/feature/dashboard/DashboardContract.kt
data class DashboardState(
    val fuelPercent: Int? = null,          // null = 아직 모름 또는 미지원
    val rangeKm: Int? = null,
    val drivingMode: DrivingMode? = null,
    val pendingRefuel: RefuelDraft? = null,
    val warnings: Set<VehicleWarning> = emptySet(),
    val isDemo: Boolean = false,
)

sealed interface DashboardEvent {
    // 사용자 입력
    data object RefuelConfirmTapped : DashboardEvent
    data class RefuelAmountEntered(val won: Int) : DashboardEvent
    // 시스템 입력 (ViewModel 내부에서 Flow를 collect해 발생)
    data class VehicleSnapshotChanged(val snapshot: VehicleSnapshot) : DashboardEvent
    data class RefuelDetected(val draft: RefuelDraft) : DashboardEvent
}

sealed interface DashboardEffect {
    data class Speak(val text: String) : DashboardEffect
    data class NavigateTo(val route: Route) : DashboardEffect
    data class ShowMessage(val text: String) : DashboardEffect
}
```

ViewModel 규칙:

- 노출은 `state: StateFlow<State>`, `effects: Flow<Effect>`, `onEvent(Event)` 세 가지뿐이다.
- `State`는 불변 data class다. 변경은 `_state.update { it.copy(...) }`로만 한다.
- 이벤트 처리 → 상태 갱신(reduce)은 가능한 한 순수 함수로 둔다. 부수 효과(use case 호출, 게이트웨이 쓰기)는 `viewModelScope`에서 실행하고, 결과는 다시 이벤트로 넣는다.
- 차량 신호는 domain의 `Flow`로 들어온다. ViewModel이 `init`에서 collect해서 시스템 이벤트로 바꾼다. 화면은 신호의 출처(실차, 폴링, Fake)를 모른다.
- `Effect`는 `Channel`로 보내고 한 번만 소비한다. 토스트, 화면 이동, TTS 발화가 여기 속한다.

Compose 규칙:

- `XxxScreen(state, onEvent)`는 stateless다. 미리보기와 테스트가 가능해야 한다.
- `XxxRoute()`가 `hiltViewModel()`을 얻어 `Screen`에 연결하고 `effects`를 처리한다.
- 화면 안에서 저장소, 게이트웨이, SDK를 직접 호출하지 않는다.

"양방향 통신"과의 관계: 차량에 쓰는 동작(경유지 추가, 경로 설정)과 서버 통신은 모두 이벤트가 일으키는 부수 효과다. UI → ViewModel은 이벤트만, ViewModel → UI는 상태와 효과만 흐른다.

### 2.3 게이트웨이: SDK 캡슐화

domain에 인터페이스를 두고 platform에서 구현한다. **Pleos SDK, AAOS Car API 타입은 `core/platform` 밖으로 나가지 않는다.**

| 인터페이스 (domain) | 역할 | 구현 (platform) |
|---|---|---|
| `VehicleGateway` | 연료 %, 연비, 트립 거리·시간, 주행 모드, 경고등, 에어백, VIN, 지원 여부 | `PleosVehicleGateway`, `FakeVehicleGateway` |
| `NavigationGateway` | 목적지·경유지 설정, 경로 이벤트(도착, 경유지 도착, 트립 정보) | `PleosNavigationGateway`, `FakeNavigationGateway` |
| `CarPropertySource` | AAOS 표준 속성: 속도, 시동 상태, 잔여 주행거리, 탱크 용량 | `AaosCarPropertySource`, `FakeCarPropertySource` |
| `SpeechInput` | 발화 → 텍스트 Flow | `PleosSpeechInput`(STT SDK), `AndroidSpeechInput`(`SpeechRecognizer`), `FakeSpeechInput` |
| `SpeechOutput` | 텍스트 발화 | `PleosSpeechOutput`, `AndroidSpeechOutput`, `FakeSpeechOutput` |
| `LocationSource` | 위치 Flow | `PleosLocationSource`(FusedLocation), `AndroidLocationSource`, `FakeLocationSource` |

구현 규칙:

- SDK 콜백은 `callbackFlow`로 감싸 `Flow`로 바꾼다. `register`/`unregister`는 Flow의 수집 시작·취소에 묶는다.
- `checkXxxCapabilities`는 게이트웨이 안에서 호출한다. 상위 계층은 `null`이나 `Unsupported` 상태만 본다. 모델명으로 분기하지 않는다.
- SDK의 `register*` 리스너를 쓸 수 없으면 게이트웨이 안에서 폴링한다. 상위 계층은 차이를 모른다.
- 값의 단위는 domain 모델에서 고정한다(km, L, %, 원). 단위 변환은 게이트웨이가 한다.
- SDK 초기화·해제(`initialize`/`release`)는 게이트웨이가 앱 프로세스 생명주기에 맞춰 관리한다. ViewModel은 모른다.

데모 모드 전환:

- Hilt는 실제 구현과 Fake 구현을 둘 다 바인딩한다. domain이 받는 것은 `SwitchingVehicleGateway` 같은 위임 객체다.
- 위임 객체는 `DataSourceMode`(REAL/DEMO) `StateFlow`를 보고 `flatMapLatest`로 소스를 바꾼다. 앱 재시작 없이 전환된다.
- `DemoScenario`는 platform에 둔다. 주유 → 주행 → 정비 경고 → 정비소 도착 → 재시동 같은 단계를 시간 순으로 Fake 게이트웨이에 흘려보낸다. 숨은 데모 화면에서 재생·일시정지·점프를 할 수 있다.
- **`if (isDemo)` 분기는 platform 안에서만 한다.** ui·domain·data에 데모 분기를 두지 않는다. 단, 화면에 "데모 모드" 배지를 표시하는 것은 허용한다(심사 정직성).

### 2.4 데이터 계층

- Room이 로컬 진실 공급원이다. 저장소는 Room의 `Flow`를 노출하고, 서버 응답은 Room에 쓴 뒤 Flow로 화면에 반영한다.
- 쓰기는 로컬에 먼저 저장하고 `syncState = PENDING`으로 둔다. `WorkManager`가 네트워크가 될 때 백엔드로 보낸다.
- DTO(Retrofit) ≠ Entity(Room) ≠ 도메인 모델. 세 종류를 섞지 않고 매퍼로 변환한다.
- 설정과 동의 상태는 DataStore에 둔다. 동의 전에는 동기화 Worker가 실행되지 않는다.
- 외부 API(오피넷, 차종 DB)는 앱에서 직접 부르지 않는다. 백엔드 엔드포인트만 호출한다.

### 2.5 캡슐화 규칙 (앱)

1. Pleos SDK, `android.car.*` 타입은 `core/platform` 밖으로 나가지 않는다.
2. UI는 ViewModel의 `State`·`Effect`만 본다. 저장소·게이트웨이·SDK를 직접 호출하지 않는다.
3. ViewModel은 use case와 저장소 인터페이스만 호출한다.
4. 공개 모델은 모두 불변이다(`data class`, `val`, 읽기 전용 컬렉션).
5. 모듈이 공개하는 것은 인터페이스, 모델, Hilt 모듈뿐이다. 구현 클래스는 `internal`로 둔다.
6. Capability 확인과 폴링·리스너 선택은 게이트웨이 안에 숨긴다.
7. 시간(`Clock`), 디스패처, 랜덤은 주입한다. 테스트에서 바꿔 끼울 수 있어야 한다.
8. 데모 분기는 platform 안에서만 한다.
9. `pleos.car.permission.*`은 실제로 호출하는 API의 권한만 선언한다. 승인 범위 밖의 권한은 앱 심사 탈락 사유다.
10. 서버로 보내는 데이터에 VIN 원문, 정밀 위치, 음성 원문을 넣지 않는다. VIN은 해시, 위치는 반올림, 음성은 파싱된 결과만 보낸다.

### 2.6 주요 의존성

버전은 `app/gradle/libs.versions.toml`에서 관리한다.

| 용도 | 라이브러리 |
|---|---|
| DI | Hilt + `hilt-navigation-compose` (KSP) |
| 비동기 | kotlinx-coroutines |
| 로컬 DB | Room (KSP) |
| 설정 | DataStore Preferences |
| 네트워크 | Retrofit, OkHttp, kotlinx-serialization-json, retrofit converter |
| 백그라운드 | WorkManager (+ Hilt worker) |
| 화면 이동 | Navigation Compose |
| 차량 | `ai.pleos.playground:Vehicle`, `NaviHelper`, `SpeechToText`, `TextToSpeech`, `FusedLocation` |
| AAOS | `android { useLibrary("android.car") }` 로 `CarPropertyManager`, `CarUxRestrictionsManager` 사용 |
| 테스트 | JUnit, kotlinx-coroutines-test, Turbine, Compose UI test |

### 2.7 테스트 전략

- domain: use case 단위 테스트. JVM에서 돌고 Android 없이 실행된다.
- ui: ViewModel 테스트. Fake 게이트웨이와 Fake 저장소를 주입해 이벤트 → 상태를 검증한다. 데모 시나리오도 같은 Fake로 검증한다.
- platform: `Pleos*` 구현은 에뮬레이터에서 수동 검증한다. Fake 구현은 단위 테스트한다.
- Compose UI 테스트는 핵심 플로우 화면만 최소로 둔다.

## 3. 백엔드

### 3.1 역할

- 차계부 기록(주유, 정비, 지출) 동기화와 보관
- 같은 차종 사용자 간 연비·주유 통계 집계와 순위
- 외부 API 프록시와 캐시(오피넷 주유소·유가). API 키를 앱에 넣지 않는다
- 차량(기기) 등록과 토큰 발급

### 3.2 디렉터리 구조

```
backend/
  prisma/
    schema.prisma
    migrations/
  prisma.config.ts
  docker-compose.yml        로컬 PostgreSQL, Redis
  src/
    main.ts
    app.module.ts
    config/                 env 스키마(zod)와 타입 안전 설정. process.env는 여기서만 읽는다
    common/                 예외 필터, 인터셉터, 파이프, 데코레이터, AppError
    infra/
      prisma/               PrismaModule, PrismaService
      redis/                RedisModule, CacheService, LeaderboardService
      queue/                BullMQ 설정, 큐 이름·job payload 타입 정의
    modules/
      auth/                 차량 등록(VIN 해시), 토큰 발급·검증, 전역 가드
      vehicles/             차량 프로필, 차종 메타(탱크 용량 등)
      fuel-logs/            주유 기록
      maintenance/          정비·지출 기록, 경고 이력
      stats/                집계 job(processor), 차종별 통계·순위 API
      poi/                  오피넷 프록시, 주유소 검색, 캐시
    generated/prisma/       Prisma 생성 코드 (gitignore)
  test/                     e2e
```

### 3.3 모듈 내부 계층

모듈 하나는 아래 파일로 구성한다.

```
modules/fuel-logs/
  fuel-logs.module.ts       providers 등록, exports: [FuelLogsService]만
  fuel-logs.controller.ts   HTTP 진입점. DTO 검증, service 호출, 응답 DTO 매핑
  fuel-logs.service.ts      use case. 트랜잭션 경계, 도메인 규칙, 다른 모듈 service 호출
  fuel-logs.repository.ts   Prisma 접근. 이 파일만 PrismaService를 주입받는다
  fuel-logs.processor.ts    (필요 시) BullMQ job 진입점. controller와 같은 급
  fuel-logs.types.ts        모듈 엔티티·값 타입 (Prisma 타입이 아닌 모듈 소유 타입)
  dto/                      요청·응답 DTO (class-validator)
```

흐름은 `controller | processor → service → repository → Prisma`로 고정한다. 역방향 호출과 단계 건너뛰기는 금지한다.

### 3.4 캡슐화 규칙 (백엔드)

1. `PrismaService`는 repository에서만 주입받는다. service·controller에서 `@prisma/client`나 `generated/prisma`를 import하지 않는다.
2. repository는 Prisma 타입을 밖으로 내보내지 않는다. `x.types.ts`의 모듈 타입으로 매핑해 반환한다.
3. 모듈의 `exports`는 service뿐이다. 다른 모듈의 repository를 직접 쓰지 않는다.
4. 모듈 간 호출은 service → service만 허용한다. 순환 의존이 생기면 `EventEmitter`나 큐로 끊는다.
5. 요청 DTO는 class-validator로 검증한다. 전역 `ValidationPipe({ whitelist: true, forbidNonWhitelisted: true, transform: true })`를 켠다.
6. 응답은 명시적 응답 DTO로만 보낸다. 엔티티나 Prisma 결과를 그대로 반환하지 않는다.
7. `process.env`는 `config/`에서만 읽는다. zod 스키마로 부팅 시 검증하고, 다른 곳은 타입이 있는 config 객체를 주입받는다.
8. 오류는 `AppError(code, message, status)`를 상속한 도메인 오류로 던진다. 전역 필터가 `{ code, message }`로 바꾼다. 스택과 내부 메시지는 응답에 넣지 않는다.
9. 모든 라우트는 기본으로 인증 가드가 걸린다. 공개 라우트는 `@Public()`으로 명시한다.
10. Redis·BullMQ 클라이언트는 `infra/`의 서비스만 직접 다룬다. 큐 이름과 job payload 타입은 `infra/queue/jobs.ts` 한 곳에 둔다.
11. 개인정보: VIN은 서버 비밀키로 HMAC한 값만 저장한다. 원문은 저장도 로그도 하지 않는다. 위치는 소수점 3자리로 반올림해 받는다. 동의 플래그가 없는 차량의 데이터는 거부한다.

### 3.5 Redis와 큐의 용도

| 용도 | 수단 | 내용 |
|---|---|---|
| 외부 API 캐시 | Redis string, TTL 10~30분 | 오피넷 주유소·유가 응답. 좌표를 격자로 반올림해 키로 쓴다 |
| 통계 캐시 | Redis hash, TTL | 차종별 평균 연비·주유 통계 |
| 순위 | Redis ZSET | `leaderboard:{modelKey}:{yyyy-MM}`. 점수는 월간 평균 연비. 상위 N과 내 순위를 O(log n)으로 조회 |
| 집계 작업 | BullMQ `stats-aggregate` | 주유 기록 저장 시 차종별로 디바운스해 enqueue. 야간 반복 job으로 전체 재집계 |
| 외부 데이터 갱신 | BullMQ `opinet-refresh` (repeatable) | 유가 주기 갱신 |
| 요청 제한 | `@nestjs/throttler` + Redis storage | 기기당 요청 제한 |

이유: 요청 경로에서 집계와 외부 호출을 빼서 응답을 빠르게 유지하고, 외부 API 호출량과 키 노출을 줄인다. 큐와 캐시를 분리해 두면 서버를 여러 대로 늘려도 구조가 바뀌지 않는다.

### 3.6 API 계약

- URL은 `/v1/...`로 버전을 붙인다.
- 인증은 차량 등록 시 발급한 Bearer 토큰이다.
- `@nestjs/swagger` 데코레이터로 스펙을 만들고, `npm run openapi`로 `docs/api/openapi.json`을 생성해 커밋한다. 앱은 이 파일을 기준으로 Retrofit 인터페이스를 작성한다.
- 응답 형식은 성공 시 본문 그대로, 실패 시 `{ code, message }`다.

### 3.7 로컬 환경

```sh
cd backend
docker compose up -d            # PostgreSQL 17, Redis 7
cp .env.example .env            # 기본값이 compose와 맞춰져 있다
npm install
npx prisma migrate dev
npm run start:dev
```

### 3.8 테스트 전략

- 단위: service를 repository mock으로 테스트한다(vitest).
- e2e: docker compose의 DB를 쓰는 테스트 DB에 마이그레이션을 적용하고 supertest로 라우트를 검사한다.
- processor: job 핸들러를 service mock으로 단위 테스트한다.

## 4. 미결정 사항

- 앱 토큰 저장 방식(EncryptedSharedPreferences 또는 Keystore 직접 사용)
- OpenAPI에서 Kotlin 클라이언트를 자동 생성할지, Retrofit 인터페이스를 수동으로 쓸지. 우선 수동으로 시작한다
- 차종 메타(탱크 용량 등) 데이터의 출처
- 데이터 모델 상세는 별도 설계 문서에서 정한다
