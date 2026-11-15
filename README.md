# Moli: 내 차로 함께 달리는 드라이버 커뮤니티

현대자동차그룹 차량용 앱 개발 공모전 출품작. Moli(몰리)는 Mobility Life의 줄임말이다. Pleos Connect(AAOS) 차량 앱, 스마트폰 앱, 서버로 이뤄진다.

차에서 꾸민 내 차가 커뮤니티 프로필이 되고, 차량이 측정한 연비로 같은 차종 운전자와 순위를 비교하며, 무드등이 연비 운전을 안내한다. 이웃과 그룹 라이딩을 하며 위치를 공유하고 워키토키로 대화하고, 다녀온 장소를 지도에 핀으로 추천하며, 사고·공사·정체 같은 도로 상황을 간단히 제보한다. 주유·충전 가격은 차량이 감지한 실제 결제 기록으로 공유한다.

제품 정의는 [PRODUCT.md](PRODUCT.md), 기능 정의는 [docs/FEATURE_DEFINITION.md](docs/FEATURE_DEFINITION.md), 기획 원고는 [docs/PROPOSAL.md](docs/PROPOSAL.md)에 있다.

| 구성 | 1차 제출 상태 |
|---|---|
| 차량 앱 | 일부 기능 구현. 아래 시연 흐름 |
| 스마트폰 앱 (커뮤니티·지도·랭킹·나) | 기획·설계 |
| 서버 (계정 연결, 이웃, 랭킹, 핀, 도로 제보, 가격 정보, 위치·음성 중계) | 유가 정보 모듈(오피넷 형식 스텁) 구현, 나머지 기획·설계 |

## 시연 흐름

모든 흐름은 Pleos Connect 에뮬레이터에서 Virtual Console로 바꾼 차량 신호를 Vehicle SDK·NaviHelper SDK로 받아 동작한다.

1. **연비 코치 무드등**: 연비 주행 가이드를 켜 두면 급가속·급감속·과속을 판단해 실내 무드등을 주황, 빨강으로 바꾼다. 안정되면 원래 색으로 돌아온다(Vehicle SDK Light).
2. **내 차 꾸미기**: 3D 차량에 랩핑을 입히고, 고른 무드등 색을 실내등에 바로 적용한다. 꾸민 차는 커뮤니티 프로필이 된다(폰 앱은 설계 단계).
3. **충전 감지와 주유·충전 기록**: 충전구가 열리고 배터리가 1kWh 넘게 오른 뒤 닫히면 기록이 생기고, 금액만 확인하면 저장된다. 월별 주행거리·지출·1 km당 비용이 쌓인다. 연비 랭킹과 가격 공유의 기준 데이터가 된다.
4. **선호 브랜드 충전소 경유**: 경로가 있고 주행 가능 거리가 `max(50km, 남은 거리 × 1.2)`보다 짧으면, 진행 방향 충전소 중 선호 브랜드를 먼저 제안하고 NaviHelper로 경유지에 넣는다.

## 실행

### APK

- 위치: [`apk/몰리_Moli_release.apk`](apk/몰리_Moli_release.apk) (release 빌드, 패키지 `com.example.pleos`)
- 설치: 에뮬레이터를 띄운 뒤 `adb install -r apk/몰리_Moli_release.apk`. 앱 목록에서 "Moli"를 연다.

직접 빌드하려면 다음과 같다.

```sh
cd apps/android
./gradlew :automotive:assembleRelease
adb install -r automotive/build/outputs/apk/release/automotive-release.apk
```

release APK는 R8로 축소되고, `apps/android/secrets.properties`에 키스토어가 없으면 디버그 키로 서명된다. 에뮬레이터에 바로 설치할 수 있다.

### 테스트 환경

| 항목 | 값 |
|---|---|
| 에뮬레이터 | Pleos Connect v2.1.0 (`RELEASE.release_connect_v2.1.0`), Android 14 (API 34), x86_64·arm64 |
| 화면 규격 | 2560×1440, 160dpi (기기 정의 `Pleos_Connect_v2_0`) |
| 앱 SDK | minSdk 28, targetSdk 37 |
| 사용 SDK | Vehicle SDK 2.3.2 (Light 제어 포함), NaviHelper SDK 3.0.5. 의존성 포함: SpeechToText·TextToSpeech 3.3.1, FusedLocation 1.1.3 |
| 개발 PC | Windows 11 (x86_64 이미지, `-gpu host`), macOS M4 Pro (arm64 이미지) |

필요한 설정:

- 별도 로그인·권한 승인 없이 설치 후 바로 실행된다. Pleos SDK 권한은 매니페스트 선언만으로 동작한다.
- 차량 신호를 바꿔 보려면 아래 Virtual Console을 쓴다. 차량 서비스가 없으면 데모 모드로 실행된다.
- 백엔드는 시연 흐름에 필요하지 않다(충전소 후보는 앱에 내장된 시연용 데이터).

### Pleos Connect 에뮬레이터

1. Android SDK에 Pleos Connect v2.1.0 시스템 이미지(API 34)를 설치하고 AVD를 만든다. 자세한 방법은 [조사 노트 11장](docs/PLEOS_PLATFORM_RESEARCH.md#11-에뮬레이터)에 있다.
2. 콜드 부팅으로 띄운다. GPU가 받쳐 주면 `-gpu host`가 빠르고, 죽으면 `-gpu swiftshader_indirect`를 쓴다.
   ```sh
   emulator -avd PleosConnect -gpu host -no-snapshot
   ```
3. Virtual Console을 연다. 콘솔 탭이 브라우저 앞에 있어야 값을 바꿀 수 있다.
   ```sh
   adb forward tcp:8088 tcp:8088    # 그다음 http://127.0.0.1:8088/
   ```
4. 첫 부팅에서 차량 값이 하나도 안 나오면 에뮬레이터를 한 번 재부팅한다. 그 밖의 문제와 해결법은 [조사 노트 11.3](docs/PLEOS_PLATFORM_RESEARCH.md)에 있다.

### 데모 모드

Pleos 차량 서비스가 없는 기기에서는 자동으로 데모 모드가 된다. 홈의 "데모로 보기"로 언제든 바꿀 수 있다. 데모 시나리오는 긴 경로와 부족한 배터리, 주행, 충전, 몇 달 뒤 주행거리 증가를 차례로 재생한다.

디버그 빌드에서는 adb로 내비 목적지를 설정할 수 있다(시연 녹화용).

```sh
adb shell am broadcast -n com.example.pleos/.debug.DebugRouteReceiver \
  -a com.example.pleos.debug.SET_ROUTE --es name 대전역 --ef lat 36.3322 --ef lng 127.4342
```

## 실제 연동과 모의 데이터

실제 차량 신호와 시연용 데이터를 구분해 적는다.

| 기능 | 출처 | 상태 |
|---|---|---|
| 주행거리, 배터리, 충전구, 기어, 남은 주행거리 | Pleos Vehicle SDK 2.3.2 | 실제 연동 (에뮬레이터 확인) |
| 경로 이벤트, 현재 위치, 경유지 추가 | Pleos NaviHelper SDK 3.0.5 | 실제 연동 (에뮬레이터 확인) |
| 충전소 후보 | 앱에 넣은 "시연용 충전소" 목록 | 모의 데이터. 화면에 시연용이라고 표시한다 |
| 충전 금액 초안 | 기본 단가 324.4원/kWh | 사용자가 바꿀 수 있는 기본값 |
| 유가·주유소 | 서버 `fuel-prices` 모듈 | 오피넷 API 형식으로 응답하는 스텁. 외부 호출 없음 |

## 구조

| 경로 | 내용 |
|---|---|
| `apps/android/` | Pleos Connect(AAOS) 차량 앱. 독립 Gradle 프로젝트 |
| `apps/backend/` | 서버. NestJS, PostgreSQL(Prisma), Redis(BullMQ). [실행 방법](apps/backend/README.md) |
| `docs/` | 기능 정의, 기획 원고, [Pleos SDK 확대 제안](docs/PLEOS_API_PROPOSAL.md), Pleos 플랫폼 조사 노트, 디자인 가이드 |
| `secret/` | sops·age로 암호화한 비밀 값. `sh secret/decrypt.sh`로 푼다 |

앱은 `automotive`(화면) → `core/domain`(규칙) ← `core/data`(Room), `core/platform`(Pleos SDK·데모) 구조다. 판단 로직은 순수 Kotlin인 `core/domain`에 있고 JVM 단위 테스트로 검증한다.
