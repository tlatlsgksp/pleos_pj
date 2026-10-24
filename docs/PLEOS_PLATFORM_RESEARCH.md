# Pleos 플랫폼·공모전 조사 노트

공모전 규정과 Pleos Connect SDK·API의 제공 범위를 정리한 조사 기록이다. 기획서 작성과 구현 판단의 근거로 쓴다.

- 조사일: 2026-10-24
- 조사 방법
  - 공모전 사이트(appdevchallenge.auto-hmg.io)의 모든 페이지, 공지 3건, 접수양식 docx를 읽었다.
  - Pleos 문서 사이트(document.pleos.ai)는 sitemap.xml의 4,317개 URL에서 SDK 관련 경로를 추리고 주요 페이지 원문을 읽었다.
  - Playground My Project 콘솔은 로그인이 필요해 사용자가 붙여넣은 내용을 기준으로 했다.
- 문서 버전 기준: Pleos SDK Release Note 2026-10-19 (sdk-261015), Pleos Connect Emulator 2.1.0

## 0. 이 문서를 읽는 법 (신뢰도와 한계)

각 항목에 아래 표시를 붙였다. 표시가 없는 서술은 ✅로 본다.

| 표시 | 뜻 |
|---|---|
| ✅ | 원문 페이지를 직접 읽고 확인함 |
| 🟡 | 원문 일부만 봤거나, 여러 원문을 조합해 추론함 |
| ❓ | 확인하지 못함. 설명회나 문의로 확인 필요 |
| ⚠️ | 출처끼리 내용이 서로 다름 |

조사의 한계. 이 문서를 믿기 전에 알아 둘 것:

1. **Dokka API 레퍼런스 상세(파라미터, 반환 타입, 단위, enum 값)는 대부분 읽지 않았다.** 메서드 이름은 사이트맵 URL에서 추출했다(부록 A). 이름은 정확하지만 시그니처는 원문으로 확인해야 한다.
2. **실제 에뮬레이터에서 동작을 검증한 항목은 하나도 없다.** "사용 가능"은 문서와 콘솔에 표시된 상태일 뿐이다.
3. My Project 콘솔 내용은 사용자가 2026-10-24에 붙여넣은 시점의 상태다. 상태(준비 중 → 사용 가능)는 바뀔 수 있다.
4. 영문 문서(/en/)를 기준으로 읽었다. 한글 문서와 표현이 다를 수 있다.
5. 공모전 온라인 설명회(10/28) 이전 조사라서, 설명회에서 바뀌거나 추가되는 규정은 반영하지 않았다.
6. 공지사항은 조사 시점에 3건이었다. 이후 공지는 반드시 다시 확인한다.

## 1. 공모전 요약

출처: [공모전 홈](https://appdevchallenge.auto-hmg.io/), [참가 가이드](https://appdevchallenge.auto-hmg.io/guide), [FAQ](https://appdevchallenge.auto-hmg.io/faq), [공지사항](https://appdevchallenge.auto-hmg.io/notice)

- 정식 명칭: 현대자동차그룹 차량용 앱 개발 공모전 (Code your App into Pleos App Market)
- 주최: 현대자동차그룹. 이용약관상 운영자는 현대자동차㈜·기아㈜이고, 운영 수탁사는 ㈜스터닝이다.
  - 팀 내부에서 "현대모비스 공모전"이라고 부르지만, 사이트 어디에도 모비스는 나오지 않는다.
- 참가: 대학(원)생·일반인·스타트업, 개인 또는 팀(대표자 포함 최대 5인). 부문은 대학생 / 일반(스타트업 포함).
- 현대자동차그룹 임직원은 참가는 가능하지만 수상 대상에서 제외된다.
- 총상금 5,100만원
  - 대상 1,000만원, 최우수 600만원, 우수 400만원: 부문별로 시상
  - 아이디어 특별상 150만원 2팀: 부문 통합
  - 숏폼 챌린지 50만원 1팀: 본상과 별도
- 수상팀 지원: Pleos App Market 탑재 준비를 지원한다. 다만 입점이나 배포는 보장하지 않는다.

### 1.1 일정 (KST)

| 날짜 | 내용 |
|---|---|
| 2026-10-21 ~ 11-15 23:59 | 접수 (이메일) |
| 2026-10-28 (수) 오후 | 온라인 설명회. 프로그램은 공모전 안내, 개발환경 소개, 질의응답. 시간과 링크는 공지 예정이고 다시보기 제공 예정 |
| 2026-11-23 | 1차 서류심사 결과 발표 (**20팀**) |
| 2026-12-03 ~ 12-04 | 2차 최종 PT (오프라인, 현대차 강남대로사옥 1층 UX스튜디오 서울) |
| 2026-12-18 | 최종 선정 발표 (10팀) |
| 2026-12-19 ~ 2027-01 | 수상팀 전시 준비 기간. 신규 개발이 아니라 영상 제작과 APK 안정화 기간 |
| 2027-02 | 그룹 행사 전시·시상, 최종 순위 발표. 현장 참석 의무 |

### 1.2 제출

- 필수: 접수양식(= 기획서). 개인정보 동의서가 포함된 docx로, [지원·제출 방법](https://appdevchallenge.auto-hmg.io/guide/submission)에서 받는다.
- 선택(가산점): 시연 영상, GitHub 저장소(소스와 APK)
  - 시연 영상은 목업, 프로토타입, 화면 녹화 모두 인정된다.
  - APK만 따로 내지 말고 저장소 안 `apk/팀명_앱이름_release.apk`에 넣는다(release 빌드). 용량이 넘치면 GitHub Releases 링크를 낸다.
  - 비공개 저장소도 되지만 심사용 계정을 Collaborator로 초대해야 한다. 이때 쓰기 권한도 같이 부여되니 주의한다.
  - README에 들어갈 내용: 앱 이름과 한 줄 소개, 실행 방법, 테스트 환경과 설정, APK 위치.
- 선택(가산점 없음): 숏폼 링크. 30초~1분 세로 영상을 Instagram 릴스나 YouTube Shorts에 올리고 `#HMG차량용앱개발공모전`을 포함한 해시태그 2~3개를 단다.
- 메일 주소와 제목
  - 주소: appdevchallenge@hyundai-partners.com
  - 제목: `[차량용 앱 개발 공모전] 팀명(개인은 성명)_대표자명`
  - 팀원 전원을 CC에 넣는다.
- 수정 제출: 제목 앞에 `[수정제출]`을 붙이고 **전체 자료와 서명한 동의서를 다시 첨부**해야 한다. 마감 전 마지막 제출본으로 심사한다.

### 1.3 접수양식(기획서) 항목

출처: `contest-application-consent.docx` (동의서 버전 1020)

- 02장 "앱 프로젝트 소개"에는 성명, 연락처, 소속을 적지 않는다(블라인드 심사).
- 분량 제한은 없다.

| 번호 | 항목 |
|---|---|
| 1 | 앱 한 줄 소개 |
| 2 | 기획 배경. 고객 니즈와 불편한 순간, 해결 필요성 |
| 3 | 주요 기능 및 차량 특화 요소 |
| 4 | 주요 사용 시나리오. 누가, 어떤 상황에서, 어떻게 |
| 5 | **개발 계획. Pleos Connect 환경에서의 구현 방법, 개발 범위, SDK/API 사용 계획** |
| 6 | 기대효과와 발전 가능성. 타깃 고객, 예상 MAU, Retention Rate, 확대 계획 |
| 7 | 참고자료(선택, 가산점). 화면 시안, 시연 영상, GitHub |
| 8 | 숏폼 링크(선택) |

### 1.4 심사 기준

1차 서류심사 기준이다. 세부 배점은 공개하지 않는다.

| 항목 | 내용 |
|---|---|
| 차량특화성 | 모바일 대비 차량 환경만의 가치. 주행·정차 맥락이나 차량 기능과 유기적으로 결합했는가 |
| 창의성 | 독창성. 운전자와 동승자의 Pain Point를 명확히 정의하고 해결했는가 |
| 기술 활용성 | **Pleos Playground가 제공하는 SDK·API를 앱의 핵심 기능과 사용 경험에 적절히 활용했는가** |
| 사업성 | 타깃 고객, 사용자 확보, 지속 이용·확대 가능성 |
| 가산점 | 시연 영상, GitHub(소스와 APK) |

- 2차 PT 심사 기준은 아직 공지되지 않았다(❓).
- 2차 PT에서는 **실제 동작하는 앱 시연(에뮬레이터 구동 화면 또는 기기 시연)이 필수**다. 출처는 공지 "참가 및 출품 유의사항".
- 최종 순위는 2차 PT 결과물로 확정된다. 이후의 추가 개발은 순위에 영향을 주지 않는다.

### 1.5 SDK·API 사용 범위 규정

**결론: "제공 SDK만 써야 한다", "특정 데이터만 써야 한다"는 금지 조항은 공모전 문서 어디에도 없다.** (✅ 홈, 가이드 8개 페이지, FAQ, 공지 3건, 이용약관, 접수양식을 전부 확인했다.)

관련 문구는 이것뿐이다.

- FAQ "어떤 SDK와 API를 쓸 수 있나요?": Vehicle, NaviHelper, TextToSpeech, SpeechToText, FusedLocation SDK. "각 SDK의 현재 제공 상태와 사용 조건은 SDK 문서 페이지에서 확인해 주세요."
  - 제공 목록을 안내하는 말이지 금지 조항은 아니다(🟡 해석).
- 심사 "기술 활용성": Pleos SDK·API를 **핵심 기능에** 써야 점수가 난다.
- 이용약관 7조: 출품작에 들어간 소스코드, 라이브러리, 데이터, 이미지 등은 적법한 사용 권한이 있어야 하고, 오픈소스는 라이선스를 지키고 출처를 밝혀야 한다.
  - 외부 라이브러리와 API 사용을 전제로 한 조항이다(🟡 해석).
- 공지 유의사항: 순수 창작물이어야 하고, 표절이나 다른 공모전 수상작은 제외된다. 제3자 권리를 침해하면 수상이 취소되고 상금이 환수된다.
- 수상작에는 주최측이 통상실시권과 4개월 우선협상권을 가질 수 있다. 저작권은 응모자에게 있다.

실질적 제약은 두 가지다.

1. 2차 PT에서 실제로 동작해야 하므로 **"준비 중" SDK에 핵심 기능을 걸면 시연 리스크가 있다.**
2. 기술 활용성 점수를 받으려면 **사용 가능한 Pleos SDK를 핵심에 써야 한다.**

## 2. Pleos 플랫폼 지도

| 구분 | 무엇 | 공모전 관련성 |
|---|---|---|
| Pleos Connect | 차량 인포테인먼트 플랫폼 (AAOS 기반) | **대상 플랫폼** |
| Pleos Connect SDK | 차량용 앱 SDK 5종 | **사용 대상** |
| Pleos Playground | 개발자 포털. My Project, SDK 권한 신청, 문서 | 프로젝트 생성 필요 |
| Pleos App Market | 차량 앱 마켓. 심사 가이드가 있음 | 수상 시 탑재 지원 |
| Gleo AI / Gleo Interface | 차량 음성 AI. 앱 함수 호출(App Schema) 연동 | 공모전 SDK 목록에 없음 (§7) |
| Fleet API | 클라우드 차량 관제 REST | 제휴 계약 필요라 사용 불가 |
| Vehicle Data API | 현대·기아·제네시스 차량 데이터 REST | **유럽 한정**이라 사용 불가 |
| SDV OS Vehicle API (Rust/C++) | 차량 OS 레벨 API (sdvos 경로) | 차량 앱과 무관 |
| Pleos Platform SDK | 별도 SDK 트리(pleos-platform-sdk 경로) | 🟡 공모전 목록에 없음. 내용은 조사하지 않음 |
| Connect Mobile | 모바일 원격 제어 앱 | 무관 |

기사에 나오는 "6가지 SDK(ADAS, Sync, Gleo AI SDK 포함)"는 Pleos 25 행사 당시 발표 내용이다. **현재 공개 문서의 Connect SDK는 5종이다.**
- 문서 사이트맵에 `connect-sdk-pleos/LLM/` 경로(45개 URL)가 있지만 entry 페이지가 비어 있다.
- `connect-sdk-service-status` 페이지는 "현재 이 경로에서 제공하지 않음"이라고 안내한다.
- 그래서 LLM SDK는 공개되지 않은 것으로 판단한다(🟡).

## 3. Pleos Connect SDK 공통

출처: [SDK 개요](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/), [Release Note](https://document.pleos.ai/en/docs/pleos-only/release-note/sdk-261015), [SDK API 정책](https://document.pleos.ai/en/docs/pleos-only/vehicle-app-planning-guide/pleos-connect-sdk-api), [API 호환성](https://document.pleos.ai/en/docs/pleos-only/vehicle-app-planning-guide/api-compatibility-availability), [My Project](https://document.pleos.ai/en/docs/pleos-only/create-my-project)

### 3.1 버전과 의존성

Maven 저장소: `https://nexus-playground.pleos.ai/repository/maven-releases/`

| SDK | 좌표 | 권한 (`pleos.car.permission.*`) |
|---|---|---|
| Vehicle | `ai.pleos.playground:Vehicle:2.3.2` ⚠️ | 도메인별 `CAR_*`(조회), `CONTROL_CAR_*`(제어) |
| NaviHelper | `ai.pleos.playground:NaviHelper:3.0.5` | `NAVI_ROUTE`, `NAVI_ROUTE_SEARCH`, `NAVI_CUSTOM_MAP` |
| SpeechToText | `ai.pleos.playground:SpeechToText:3.3.1` | `STT_SERVICE` |
| TextToSpeech | `ai.pleos.playground:TextToSpeech:3.3.1` 🟡 | `TTS_SERVICE` (콘솔 기준) |
| FusedLocation | `ai.pleos.playground:FusedLocation:1.1.2` | `FUSED_LOCATION` (dangerous) |

⚠️ Vehicle 버전이 출처마다 다르다.
- Release Note와 Getting Started: 2.3.1
- Vehicle 임포트 페이지와 샘플앱: 2.3.2 (Vehicle Service 2.3.21 기준)

빌드가 되는 쪽으로 정하면 된다.

🟡 TTS 좌표는 다른 SDK의 명명 규칙에서 추정했다. TTS 임포트 페이지는 읽지 않았다.

개발 환경: minSdk 28 이상(보안 요구사항이라 필수), Gradle 8.0 이상, Kotlin 또는 Java, compile/target은 API 34 에뮬레이터 기준이다.

### 3.2 API 정책

- **AAOS 표준 우선**: AAOS 표준에 같거나 비슷한 API가 있으면 표준을 쓰고, SDK는 중복 기능을 제공하지 않는다.
  - 예: 차속, 기어, 외기온, 연료량 같은 표준 데이터는 AAOS Car API로 받는다.
  - SDK는 제조사 전용 기능만 제공한다.
- SDK 기능이 AAOS에 편입되더라도 최소 2개 메이저 버전 동안 하위 호환을 보장한다.
- 차종마다 지원이 다르다. Vehicle은 `checkXXXCapabilities`, FusedLocation은 `isSupported()`로 확인한다. **모델명으로 분기하지 않는다.**
- NaviHelper는 내비가 탑재된 차량에서만 완전히 호환된다. TTS와 STT는 완전 호환이다.
- **App Market 승인 범위를 넘는 권한을 선언하거나 해당 API를 호출하면 앱 심사에서 탈락한다.** 앱마켓 출시 기준이며 공모전 기준은 아니다.
- 하드웨어 직접 접근은 금지다. 차량 상태는 SDK나 플랫폼 API로만 받는다.
- 전화, SMS, 일부 센서(자이로, 근접), 캘린더, 연락처는 차량에서 제한될 수 있다.
- 주행 중 네트워크가 끊길 수 있으니 캐싱, 재시도·타임아웃, 오프라인 안내 UI를 권장한다.

### 3.3 Playground My Project와 SDK 권한 상태

출처: My Project 문서(✅)와 사용자가 붙여넣은 콘솔 화면(2026-10-24)

- 프로젝트를 만들면 Project ID(앱마켓 심사용)와 **Client ID / Client Secret**(TTS·STT 서버 연결용)을 받는다.
- 에뮬레이터 2.1.0부터 CRN은 필요 없다.
- 상태 종류: Ready to Use(바로 사용), Request Required(신청 필요), Under Review, Rejected, **Not Available Yet(준비 중, 신청 버튼 없음)**, Affiliation Required(제휴 계약 필요)

| SDK / 기능 | 콘솔 상태 | 권한 |
|---|---|---|
| Vehicle State (조회 11종) | ✅ 사용 가능, 신청 불필요 | `CAR_*` |
| NaviHelper 경로 정보 조회 | ✅ 사용 가능 | `NAVI_ROUTE` |
| NaviHelper 경로 탐색 설정 | ✅ 사용 가능 | `NAVI_ROUTE_SEARCH` |
| SpeechToText | ⏳ 준비 중 | `STT_SERVICE` |
| TextToSpeech | ⏳ 준비 중 | `TTS_SERVICE` |
| FusedLocation | ⏳ 준비 중 | `FUSED_LOCATION` |
| Vehicle Control (제어 8종) | ⏳ 준비 중 | `CONTROL_CAR_*` |
| Fleet (차량 관제) | 제휴 계약 필요 | - |
| Vehicle Data API | 별도 신청 페이지, **유럽 한정** | - |

우리 프로젝트(2026-10-24 생성):
- 이름 "AutoMotive", 카테고리 모빌리티 서비스, 개발사 프마노
- Project ID: `8a5323da-1694-4e0a-b765-861df94b78d8`
- Client ID와 Client Secret은 문서에 적지 않는다. `app/local.properties`(gitignore 대상)의 `pleos.clientId`, `pleos.clientSecret`에 둔다.
- 콘솔에는 CRN과 데브박스 입력용 adb 명령도 나온다. 에뮬레이터 2.1.0부터는 CRN이 필요 없다.
- Vehicle Data API는 신청 시 **법인 인증이 필요**하고 유럽 지역 전용이다(사용자 확인). 공모전에서는 쓰지 않는다.

My Project 문서에도 "FusedLocation, TTS, STT, Vehicle Control은 Not Available Yet이고, Vehicle State와 NaviHelper는 신청 없이 바로 사용 가능"이라고 같은 내용이 적혀 있다(✅).

❓ "준비 중"이 **에뮬레이터에서도 동작하지 않는다는 뜻인지**, 아니면 **앱마켓 권한 발급만 막혀 있다는 뜻인지**는 확인하지 못했다.
- 문서는 STT ON_DEVICE 모드에 앱 등록이 필요 없다고 하고, 에뮬레이터 마이크 설정 안내까지 있다.
- 그래서 에뮬레이터에서는 동작할 가능성이 있다(🟡). 실제 테스트가 필요하다.

## 4. Vehicle SDK

출처: [Intro](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/Vehicle/intro), [Permission](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle-permission), Callback, Capability, Error code 페이지, [샘플앱](https://github.com/Pleos-Playground/pleos-sample-vehicle-app)

### 4.1 사용 패턴

```kotlin
val vehicle = Vehicle(context)
vehicle.initialize()                    // Vehicle Service와 IPC 연결
val fuel = vehicle.getFuel()            // 도메인 객체는 getXxx()로 얻는다 (🟡 getFuel 이름은 패턴에서 추정)
fuel.getFuelPercent(onComplete = { v -> }, onFailed = { e -> })   // 결과는 모두 비동기 콜백
// 구독: registerXxx(listener) / unregisterXxx(listener) → listener.onXxxUpdated(), onFailed()
// 지원 확인: checkXxxCapabilities(onComplete = { config: CapabilityConfig, areaMap -> }, onFailed = {})
vehicle.release()                       // 콜백이 끝난 뒤, 예를 들어 onDestroy에서 호출
```

- `CapabilityConfig.accessType`: NONE / READ / WRITE / READ_WRITE. 영역별 범위는 `AreaBounds(min, max)`로 온다.
- 에러는 `VehicleException.errorCode`로 받는다.
  - `ERROR_NOT_SUPPORTED_PROPERTY`, `ERROR_NOT_SUPPORTED_AREA`, `ERROR_NOT_SUPPORTED_VALUE_TYPE`
  - `ERROR_SECURITY_PERMISSION`, `ERROR_SERVICE_CONNECTION_FAILURE`, `ERROR_INTERNAL_SERVICE`
  - `ERROR_DRIVING_RESTRICTED`, `ERROR_EMPTY_RESULT`, `ERROR_UNDEFINED`
- 권한은 domain별로 선언한다. 예: `<uses-permission android:name="pleos.car.permission.CAR_ENERGY"/>`
  - 샘플앱 README에 따르면 normal 보호 수준이라 런타임 다이얼로그가 필요 없다(🟡).

### 4.2 도메인과 권한

| 도메인 | 내용 | 조회 권한 |
|---|---|---|
| Brake | 브레이크 페달, 주차 브레이크 | CAR_DRIVE |
| CarInfo | VIN, 제원 | CAR_INFO |
| Display | 온도 단위 | CAR_INFO |
| Door | 도어 위치, 잠금, 글로브박스 | CAR_DOORS |
| DrivingMode | 드라이브 모드, 크루즈 컨트롤 | CAR_DRIVE |
| EvBattery | EV 충전량, 전비, 충전 설정 | CAR_ENERGY |
| Fuel | 연료 잔량, 연비 | CAR_ENERGY |
| HVAC | 공조 | CAR_CLIMATE |
| Light | 외부등, 실내등, 앰비언트 | CAR_LIGHTS |
| Odometer | 누적·트립 거리와 시간 | CAR_DRIVE |
| Safety | 경고등, 에어백, 오일류, 안전벨트 | CAR_SAFETY |
| Seat | 시트 위치, 열선, 통풍, 점유 | CAR_SEATS |
| SideMirror | 미러 열선 | CAR_MIRRORS ⚠️ (콘솔 표기: CAR_MIRROR) |
| Steeringwheel | 조향각, 열선 | ❓ |
| Tire | 저압 경고 | CAR_SAFETY |
| Toll | ETCS 음성 볼륨 | CAR_TOLL |
| TurnSignal | 방향지시등 | CAR_LIGHTS |
| Window, Wiper | 창문, 블라인드, 스마트글라스, 와이퍼 | CAR_WINDOWS |

### 4.3 콘솔 "Vehicle State"에 사용 가능으로 나온 조회 API

사용자가 붙여넣은 콘솔 내용이다. 모든 항목의 "주행 중 호출 제한"은 `-`(제한 없음)이다.

| 콘솔 항목 | 권한 | API |
|---|---|---|
| 차량 제원 정보 | CAR_INFO | `getVINInfo`, `getCurrentTemperatureDisplayUnit` |
| 차량 주행 정보 | CAR_DRIVE | `getCurrentDrivingModeState`, `getTripCumulativeDrivingDistance`(누적 주행거리), `getTripCumulativeDrivingTime`, `getTripCurrentDrivingDistance`, `getTripCurrentDrivingTime`, `getTripInstantaneousDrivingDistance`, `getTripSinceRefillDrivingDistance`(주유 후 거리), `getTripSinceRefillDrivingTime` |
| 도어/트렁크/프렁크 | CAR_DOORS | `getCurrentGloveBoxLockState`, `getCurrentDoorPosition`, `getDoorLockState` |
| EV 및 연료 | CAR_ENERGY | `getFuelPercent`, `getInstantaneousFuelEconomy`, `getCurrentFuelEconomy`, `getCumulativeFuelEconomy`, `getSinceChargeFuelEconomy`(마지막 주유 이후 연비), `getEvBatteryStateOfCharge`, `getInstantaneous/Current/Cumulative/SinceChargeEvEfficiency`, `getEvChargeCurrentDrawLimit`, `getEvChargeSwitchState` |
| 공조 | CAR_CLIMATE | `getHvac*` 15개 (AutoRecirculation, Auto, Defroster, Ac, AirCleaner, Dual, FanDirection, FanSpeed, MaxAc, MaxDefrost, Power, Recirculation, Temperature, TemperatureCurrent, SteeringWheelHeat) |
| 라이트 | CAR_LIGHTS | 앰비언트 5개, 실내등·독서등, 전·후 안개등, 상향·하향등, 후미등 상태와 스위치, `getCurrentTurnSignalState` |
| 안전 | CAR_SAFETY | `getLowTirePressure`, `hasPowerDownWarningIndicator`(엔진 이상), `hasSeatAirbagEnabled`(true면 에어백 이상), `hasSeatBeltNotBuckledWarning`, `hasSeatAirbagBeenDeployed`(에어백 전개), `hasLowWasherFluidWarning`, `getEngineOilLevelState`, `getBrakeFluidLevelState`, `getTransmissionOilLevelState` |
| 좌석 | CAR_SEATS | 시트 위치·각도 12개, `getCurrentSeatBeltBuckledState`, `getCurrentSeatOccupancy`, `getHvacSeatTemperature`, `getHvacSeatVentilationState` |
| 사이드미러 | CAR_MIRROR | `getHvacSideMirrorHeatState` |
| 통행료 | CAR_TOLL | `getCurrentEtcsVoiceGuidanceVolume` |
| 창문 | CAR_WINDOWS | `getCurrentSmartGlassLevel`, `getCurrentSmartGlassState`, `getCurrentWindowBlindPosition`, `getCurrentWiperSwitch`, `getCurrentWiperState`, `getCurrentWindowPosition`, `getCurrentWindowLockState` |

콘솔의 Vehicle Control(준비 중) API는 `setDoorPosition`, `setDoorLock`, `setHvac*`, `setRangeRemainingDistance`, `setEvCharge*`, `setWiperMode`(⚠️ 문서 이름은 `setWiperSwitch`), `setWindow*`, `moveSeat*`, `selectSeatMemory`, `setHvacSideMirrorHeat`, 라이트 스위치 set들이다. 주행 중 호출 제한이 "제한"인 항목이 많다. 우리 앱은 제어를 쓰지 않는다.

### 4.4 문서에는 있는데 콘솔 목록에 없는 것 ⚠️

전체 목록은 부록 A-1을 본다.

- **`register*` / `unregister*` 리스너 전부**: 콘솔에는 get만 있다.
  - 같은 권한(`CAR_*`) 안에 포함되는지 확인해야 한다(❓).
  - 안 되면 get을 주기적으로 폴링한다.
- `getCurrentBrakePedalStatus`, `getCruiseControlState`, `getCruiseControlTargetSpeed`: 콘솔 "차량 주행 정보" 설명에는 브레이크와 크루즈가 언급되는데 API 목록에는 없다.
- `getCurrentDistanceDriven`(Odometer): 콘솔에 없다.
- `getTelltalesInfo`, `registerTelltales`: 경고등 묶음 조회(2.3.x 신규 모델)인데 콘솔에 없다.
- `getCurrentSteeringWheelAngle`, `getHvacSteeringWheelHeatState` 등 Steeringwheel 도메인
- `getCRNInfo`: deprecated다. `getVINInfo`로 이동한다.
- **잔여 주행거리 조회 API가 없다.**
  - `RangeRemainingListener`는 2.3.1에서 "문서에서 숨김(동작은 유지)" 처리됐다.
  - 콘솔에는 `setRangeRemainingDistance`(제어, 준비 중)만 있다.
  - 잔여거리는 AAOS `RANGE_REMAINING`이나 자체 계산(§9)으로 처리한다.
- **시동(ignition)과 차속 API가 없다.** AAOS 표준으로 처리한다(§8).

### 4.5 2.3.x 주의사항 (Release Note)

- `CruiseControl` 상태 enum이 OTHER, ENABLED, ACTIVATED, USER_OVERRIDE, SUSPENDED, FORCED_DEACTIVATION_WARNING로 변경됐다.
- `FanDirectionType`은 8개 값으로 늘었고 ordinal이 바뀌었다.
- Area 클래스(DoorArea 등)가 값 기반 equals로 바뀌었다.
- `VehicleSpeedListener`는 **삭제**됐다. 차속은 AAOS `PERF_VEHICLE_SPEED`를 쓴다.
- 신규: `getVINInfo`, `getCurrentEvEfficiency`, `registerEvBatteryStateOfCharge`, 모델 `TripStatistics`, `TelltaleData`, `Telltales`, `CapabilityConfig`

## 5. NaviHelper SDK (v3.0.5)

출처: [Intro](https://document.pleos.ai/en/api-reference/connect-sdk-pleos/NaviHelper/intro), Destinations, Routes, Waypoints, Route with waypoints, Event listeners, Permissions 페이지

- 순서: `NaviHelper(context)` → `initialize(onConnected, onFailed)` → `addListener(NaviHelperEventListener)` → API 호출 → `removeListener` → `release()`
- **조회 API는 값을 반환하지 않는다.** 결과는 대응하는 콜백(`getDestinationInfo` → `onDestinationInfo`)으로 온다.
- `requestRoute` 성공이 경로 탐색 완료를 뜻하지는 않는다. 완료는 `onRoutingCompleted`로 확인한다.
- 목적지: `requestRoute(RouteInfo(latitude, longitude, displayLongitude?, displayLatitude?, poiName, poiId, poiSubId, address[, routeOption]))`
  - 3.0.5부터 생성자 좌표 순서가 **lat, lng**로 바뀌었다. 문서의 Java 예제는 아직 옛 순서로 남아 있으니 주의한다.
- 경유지는 **최대 2개**(`WaypointIndex.FIRST`, `SECOND`)다. `addWaypoint(RequestWaypointInfo(...))`, `removeWaypoint(index | ALL)`을 쓴다.
- 경유지 포함 경로는 `requestRouteWithWaypoints(RouteWithWaypointsRequest(destination, waypoints))`로 요청한다.
- 경로 옵션은 `RouteOption`이다: RECOMMENDED, SHORT_DISTANCE, SHORT_TIME, FREE, HIGHWAY, COMFORTABLE, SCHOOL_ZONE. `changeRouteOption`과 `requestReRoute`는 활성 경로가 있을 때만 쓴다.
- 권한
  - `NAVI_ROUTE`: `get*Info()`, `requestTrip()`
  - `NAVI_ROUTE_SEARCH`: `requestRoute`, `requestRouteWithWaypoints`, `cancelRoute`, `requestReRoute`, `addWaypoint`, `removeWaypoint`, `changeRouteOption`
  - `NAVI_CUSTOM_MAP`: `requestNavigationScreen()`. ⚠️ 콘솔 목록에 없다.
- 이벤트(`NaviHelperEventListener`)
  - 경로: `onRouteStarted`, `onRoutingCompleted`, `onRouteCancelled`, `onReRouteEnded`, `onRouteOptionChanged`
  - 목적지·경유지: `onDestinationChanged`, **`onDestinationArrived`**, `onWaypointChanged`, **`onWayPointArrived`**
  - 정보: `onBookmarkInfo`, `onBookMarkChanged`, `onRecentDestinationInfo`, `onCurrentLocationInfo`, `onDestinationInfo`, `onWayPointInfo`, `onRouteStateInfo`, `onTBTInfo`, `onChargerOperatorInfo`, **`onTripInfo`**, `onNavigationStatus`, `onDrivingInfo`, `onCurrentRoadSpeedLimit`, `onCameraAlert`, `onCautionPointInfo`
  - 오류: `onError(NaviErrorCode)`
  - 어떤 이벤트가 오는지는 내비 앱의 구현과 상태에 따라 달라질 수 있다고 문서에 적혀 있다.
- **POI(주유소·정비소) 검색 API는 없다.** 좌표와 POI 정보는 외부 API로 구해야 한다. `getChargerOperatorInfo`는 EV 충전 사업자 정보다.
- ⚠️ 콘솔과 문서가 다르다.
  - 콘솔에는 `findRoute`, `startRouteGuiding`이 있다. 문서 3.0.5에는 없고, `FindRouteResult`와 `onUserDefinedRoute`는 삭제됐다.
  - 문서에만 있는 것: `requestRouteWithWaypoints`, `requestTrip`, `requestNavigationScreen`
  - 콘솔이 옛 버전 기준일 가능성이 있다(🟡).
- ❓ 이벤트 콜백 수신에 별도 권한이 필요한지 문서에 명시돼 있지 않다.

## 6. SpeechToText / TextToSpeech / FusedLocation

모두 콘솔 상태는 "준비 중"이다.

### 6.1 SpeechToText (v3.3.1)

- 음성을 텍스트로 바꿔 돌려주기만 한다. **명령 실행 기능은 없다.** 명령 실행은 Gleo Interface의 영역이다.
- 모드
  - `HYBRID`(기본): 서버 우선, 실패하면 온디바이스로 전환한다. 앱 등록이 필요하다.
  - `SERVER`: 앱 등록이 필요하다.
  - `ON_DEVICE`: 네트워크와 앱 등록 모두 필요 없다.
- 흐름: `SpeechToText(context, Mode)` → `initialize(OnInitializeListener)` → `addListener(ResultListener)` → `setLocale` → (서버 모드면 `registerApp(clientId, clientSecret, OnServerConnectionListener)`) → `request()` → `stop()` → `removeListener` → `release()`
- 저장된 PCM 오디오를 변환할 때는 `sendAudio`, `completeAudioSend`를 쓴다.
- 지원 locale: ko-KR, en-US, fr-CA, es-US, es-MX
- 에러는 `ErrorType` enum이다(3.3.1에서 String에서 변경됨). `InferenceSource`는 ON_DEVICE / SERVER다.
- 에뮬레이터: Extended Controls > Microphone에서 오디오 입력을 켜야 한다.

### 6.2 TextToSpeech (v3.3.1)

- 콘솔 API: `setAudioAttributes`, `speak`, `stop`
- 사이트맵 기준 추가 API: `setLocale`, `setSpeakerType`, `setAudioZoneId`, `registerApp`, `addEventListener` 등(부록 A-5)
- intro 페이지만 일부 읽었고 상세는 읽지 않았다(🟡).

### 6.3 FusedLocation (v1.1.2)

- 차량 GPS, IMU, 카메라를 합친 위치다.
- API: `isSupported()`, `initialize`, `getLastKnownLocation`, `registerFusedLocationCallback` / `unregister...`, `addProximityAlert` / `removeProximityAlert`(지오펜스), `showLocationAccessBlockedPopup`, `release`
- `LocationData`: 위도, 경도, 고도, 방향, 신뢰도, 타임스탬프
- 콜백은 null을 받을 수 있고 `onError`를 반드시 구현해야 한다.
- 권한 `FUSED_LOCATION`은 dangerous 등급이라 런타임 요청이 필요하다.
- Android 표준 위치 권한의 동작은 이 SDK 문서가 보장하지 않는다.

## 7. Gleo Interface (공모전 SDK 목록 밖)

출처: [Core concepts](https://document.pleos.ai/en/docs/pleos-only/app-integration-guide/guide/core-concepts), [App Schema](https://document.pleos.ai/en/docs/pleos-only/app-integration-guide/guide/app-capability), [Callback](https://document.pleos.ai/en/docs/pleos-only/app-integration-guide/guide/action_callback), [Examples](https://document.pleos.ai/en/docs/pleos-only/app-integration-guide/guide/examples)

나중에 쓸 수도 있어서 기록해 둔다. **공모전 FAQ의 SDK 목록에 없고, 등록 절차나 에뮬레이터 지원 여부는 확인하지 못했다(❓). 기획의 핵심으로 쓰지 않는다.**

- Gleo AI가 음성 원문을 앱에 넘겨주는 방식이 **아니다.** function calling 방식이다.
- 앱이 App Schema(JSON)로 함수를 등록하면 Gleo가 그 함수를 호출한다.
- App Schema 필드: `packageName`, `appDescription`(영어), `invokeWord`, `version`, `actionCalls`(**최대 3개**)
  - 함수 이름은 UPPER_SNAKE로 쓴다.
  - 파라미터 타입은 string, number, boolean, array만 된다. enum은 string만 된다.
  - `component`는 "activity"(startActivity) 또는 "service"(sendBroadcast)다.
- 호출 방식: Intent action이 함수 이름이고, `ACTION_PARAM` extra에 파라미터 JSON 문자열이 들어온다.
- 응답 방식
  - `Intent("ai.pleos.playground.intent.action.APP_ACTION_RESPONSE")`에 `packageName`, `response` extra를 담아 브로드캐스트한다.
  - `response` 형식: `{functionName, status: success|asking|fail, message(영어), data(JSON 배열 문자열), mode: voice|beep|silent}`
  - **7초 안에 응답하지 않으면 타임아웃**이다.

## 8. AAOS 표준 API (Pleos SDK 밖이지만 공식 권장)

- Pleos 공식 FAQ의 "Checking Cluster Speed"는 클러스터와 같은 차속을 얻으려면 `CarPropertyManager`로 `PERF_VEHICLE_SPEED`를 읽으라고 직접 안내한다(✅).
  - 즉 AAOS 표준 API 사용은 공식적으로 허용된 방식이다.
- 지원 여부는 차종마다 다르다. `VehiclePropertyIds`와 `CarPropertyManager`로 실제 지원을 확인하고, 모델명으로 분기하지 않는다(✅ API 호환성 문서).
- "SDK에 없는 특수 속성이 필요하면 구현 전에 기술지원팀 검토를 요청하라"는 문구가 있다(✅).
  - 표준 속성까지 해당하는지는 불명확하다(❓).
- 우리 앱에서 쓸 후보 (🟡 Pleos 차량·에뮬레이터 지원은 미확인)
  - `PERF_VEHICLE_SPEED`(차속): 운전 점수
  - `IGNITION_STATE`(시동): 재시동 감지
  - `RANGE_REMAINING`(잔여 주행거리)
  - `GEAR_SELECTION`, `PERF_ODOMETER`, `FUEL_LEVEL`, `INFO_FUEL_CAPACITY`(탱크 용량), `INFO_MAKE` / `INFO_MODEL` / `INFO_MODEL_YEAR`
- 권한은 `android.car.permission.CAR_SPEED`, `CAR_POWERTRAIN`, `CAR_ENERGY`, `CAR_INFO` 등이다. 표준 권한이고 일부는 시스템 권한일 수 있다(🟡).
- 주행 제한은 `CarUxRestrictionsManager`로 처리한다(✅ Pleos 주행 규제 개발 가이드).
  - Distraction Optimization(DO)을 선언하지 않은 앱은 주행 중 사용할 수 없다.
- 기기 식별: `android.os.Build.DEVICE`로 CONNECT_S / CONNECT_L / CONNECT_W를 구분한다(실차에서만 값이 나옴).
- 지역 식별: `product.sw.target_region`으로 KOR / EUR / USA / CAN을 구분한다.

## 9. 우리 앱(차계부) 기능 매핑

| 기능 | 구현 수단 | 상태 |
|---|---|---|
| 연비·주행모드별 연비 기록 | Vehicle `getCurrentDrivingModeState`와 `getInstantaneousFuelEconomy`를 주기적으로 샘플링 | ✅ 사용 가능 API |
| **주유 자동 감지** | `getTripSinceRefillDrivingDistance` 초기화와 `getFuelPercent` 상승을 함께 감지 → 금액 입력 요청 | ✅ 사용 가능 API (🟡 리셋 동작은 미검증) |
| 주행거리 자동 기록 | `getTripCumulativeDrivingDistance`, 트립 거리·시간 | ✅ |
| 정비 알림 | `getEngineOilLevelState`, `getBrakeFluidLevelState`, `getTransmissionOilLevelState`, `getLowTirePressure`, `hasLowWasherFluidWarning`, `hasPowerDownWarningIndicator` | ✅ |
| 사고 시 보험사 연락 권유 | `hasSeatAirbagBeenDeployed`. 충돌 감지 API는 없다 | ✅ 에어백만 |
| 같은 차종 통계·순위 | `getVINInfo`로 차종을 식별하고 자체 서버에서 집계 | ✅ API + 자체 서버. 개인정보 동의 필요 |
| 정비소 도착 → 재시동 시 정비 내용 질문 | NaviHelper `requestRoute` + `onDestinationArrived` → 다음 시동 때 알림. 시동은 AAOS `IGNITION_STATE`나 부팅·앱 시작으로 감지 | ✅ Navi / 🟡 시동 |
| 잔여거리 부족 시 주유소 표시 | 잔여거리 ≈ `getFuelPercent` × 탱크용량(차종 DB 또는 AAOS `INFO_FUEL_CAPACITY`) × `getCurrentFuelEconomy`. 또는 AAOS `RANGE_REMAINING`. 주유소 목록과 가격은 오피넷 API, 경유지는 `addWaypoint` | ✅ Navi / 🟡 계산·외부 API |
| 경로·운전 점수 | NaviHelper `onTripInfo`, `onDrivingInfo`, `onCurrentLocationInfo` + AAOS 차속 | 🟡 이벤트 페이로드 미확인 |
| 음성 정비·지출 기록 | STT SDK(준비 중)를 1순위로 두고, 안 되면 Android `SpeechRecognizer`나 외부 STT로 대체한다. 파싱은 자체 구현 | ⏳ / ❓ 에뮬레이터 동작 미확인 |
| 오토큐·블루핸즈 예약 | 공개 API 없음(❓). 웹 링크나 전화 연결 | 외부 |
| 보험 긴급출동 | Pleos API 없음. 보험사 전화나 앱 딥링크 | 외부 |
| 차량 꾸미기·모델링 | 제공 API 없음. 자체 에셋 | 자체 |
| 대여 모드 | 제공 API 없음. 앱 로직 | 자체 |

앱 카테고리 추천은 **Mobility Services**다.
- 카테고리 문서의 "주행 중 제한적으로 허용될 수 있는 기능" 예시에 "주행과 밀접한 차량 관리 서비스"와 "주변 주유소 검색"이 그대로 있다(✅).

## 10. 정책 체크리스트 (앱마켓 심사 기준, 기획서 반영용)

- 주행 중 UX: `CarUxRestrictions`를 따른다. 주행 중 키보드 입력은 막히고 검색은 음성만 된다(지역별 표 참고). 주행 중 관련 없는 헤드업 알림은 금지된다.
- 개인정보
  - 수집 항목과 목적을 앱 안에 명시하고, 동의 버튼으로만 동의를 받는다.
  - 동의를 철회하면 데이터를 즉시 삭제한다.
  - 차량 ID와 사용자 ID는 가명화한다.
  - 주행 행동 데이터는 마케팅과 분리한다.
  - 위치 데이터를 저장하는 서비스는 국가별 신고나 등록이 필요할 수 있다(국내는 위치정보사업 신고 검토 필요, 🟡).
- 차량은 공유 기기이므로 계정 전환과 로그아웃을 쉽게 만든다. 토큰은 Keystore에 저장한다.

## 11. 에뮬레이터

출처: [설치](https://document.pleos.ai/docs/connect/guide/getting-started/application-development/setup-connect-sdk-emulator), [설정](https://document.pleos.ai/en/docs/connect/guide/getting-started/application-development/configure-pleos-connect-emulator), [Virtual Console](https://document.pleos.ai/en/docs/connect/guide/getting-started/application-development/use-vehicle-emulator)

- 설치: Android Studio SDK Manager > SDK Update Sites에 아래 주소를 추가한다.
  - Windows/Linux: `https://nexus-playground.pleos.ai/repository/raw-releases/connect-avd/2.x/crp-sys-img.xml`
  - Mac: `.../crp-arm64-sys-img.xml`
  - 그다음 API 34의 Pleos Connect v2.1.0 이미지를 설치한다.
- AVD: Automotive 폼팩터, RAM 4GB, **Cold boot 필수**(Quick boot면 VHAL 사용 불가). 기본 언어는 한국어다.
- GPU 요구사항
  - 권장: RTX 4060/5060 Laptop 8GB급
  - 최소: RTX 3060 Laptop 6GB 또는 RX 6600M 8GB
- Virtual Console
  - `adb forward tcp:8088 tcp:8088` 후 브라우저에서 `http://127.0.0.1:8088/`을 연다.
  - 시뮬레이션 영역:
    - Vehicle: 알림
    - Motion: 기어, 브레이크, 조향, **주행거리**, 핸들 열선
    - ADAS: 크루즈
    - Convenience: 도어, 창문, 트렁크, 시트, 라이트, 와이퍼, ETCS, 글로브박스
    - Safety: 경고등. 주차 브레이크, 안전벨트, ABS 등
    - Energy: **잔여 주행거리**, 배터리, 충전
    - Climate: 공조
  - ❓ **연비, 연료량(%), 주행모드, 에어백 전개를 콘솔에서 조작할 수 있는지는 확인하지 못했다.** 시연 시나리오의 핵심이므로 Virtual Console 사용자 가이드나 실제 실행으로 확인해야 한다.
- 블루투스 동글은 지원하지 않는다. 미디어 코덱은 실차와 다를 수 있다.

## 12. 미해결 질문 (설명회 Q&A나 메일 문의용)

우선순위 순이다.

1. "준비 중" SDK(STT, TTS, FusedLocation)를 공모전 개발과 시연(에뮬레이터)에 쓸 수 있는가? 기획서에 "공개 시 연동 예정"으로 적어도 되는가?
2. AAOS 표준 `CarPropertyManager`(차속, 시동, 잔여 주행거리)와 외부 API(오피넷 등), 자체 서버를 써도 심사에 불이익이 없는가?
3. Vehicle `register*` 리스너가 콘솔 목록에 없다. `CAR_*` 권한으로 쓸 수 있는가?
4. NaviHelper 이벤트(`onDestinationArrived`, `onTripInfo`)를 받으려면 어떤 권한이 필요한가? 콘솔의 `findRoute`/`startRouteGuiding`과 문서 3.0.5의 API가 다른데 어느 쪽이 기준인가?
5. Gleo Interface(App Schema) 연동을 공모전에서 쓸 수 있는가? 등록 절차는 무엇인가?
6. Virtual Console에서 연비, 연료량, 주행모드, 에어백 전개를 시뮬레이션할 수 있는가?
7. 2차 PT 심사 기준은 무엇인가? (공지 예정)

## 13. 목데이터·스텁 시연 전략

### 13.1 규정상 허용 여부

- 1차: 시연 영상은 "목업, 프로토타입, 화면 녹화 모두 인정"이다(✅ 모집 공고).
- 2차: "실제 동작 가능한 앱 시연(에뮬레이터 구동 화면 또는 기기 시연) 필수"다(✅ 유의사항 공지).
  - 데이터가 실제 차량이나 SDK에서 와야 한다는 조항은 없다.
  - 실차가 없는 참가자를 전제로 에뮬레이터 시연을 허용한 것이다.
- 따라서 **목데이터나 스텁 기반 시연은 규정 위반이 아니다**(🟡 명시 조항이 없어서 해석함).
- 단, 제출 서류가 사실과 다르면 선정이 취소될 수 있다(✅ 유의사항). **어디까지가 실제 SDK 연동이고 어디부터 목인지 기획서와 PT에 정직하게 표시해야 한다.**
- 기술 활용성 점수를 생각하면 "사용 가능" SDK까지 목으로 대체하는 건 손해다.

### 13.2 원칙

| SDK / 데이터 | 시연 방식 |
|---|---|
| Vehicle State, NaviHelper (사용 가능) | **실제 SDK를 호출**하고, 값은 Virtual Console로 조작한다. 콘솔에서 못 바꾸는 값(❓ 연비, 주행모드, 에어백 등)만 목으로 보충한다 |
| STT, TTS, FusedLocation (준비 중) | 인터페이스 뒤에 실제 구현과 스텁 구현을 둔다. 실제 SDK가 에뮬레이터에서 동작하면 실제를, 아니면 스텁(또는 Android 표준 `SpeechRecognizer`, `TextToSpeech`)을 쓴다 |
| 외부 데이터(오피넷, 차종 통계 서버, 정비소 예약) | 실제 API를 쓰되, 오프라인 시연을 위해 녹화된 응답(fixture)으로 대체할 수 있게 한다 |

구현 형태:
- `VehicleDataSource`, `NavigationGateway`, `SpeechInput`, `SpeechOutput`, `LocationSource` 같은 인터페이스를 둔다.
- 구현체는 `Pleos*`(실제), `Fake*`(스텁·시나리오 재생) 두 가지다.
- 빌드 플레이버나 설정 화면의 "데모 모드" 토글로 전환한다.
- 데모 모드에는 시나리오 재생 기능을 넣는다(주유 → 주행 → 정비 경고 → 정비소 도착 → 재시동). PT 시간 안에 핵심 플로우를 확실하게 보여주기 위해서다.
- 스텁의 시그니처는 공식 문서(부록 A)를 따른다. 그래야 SDK가 공개됐을 때 구현체만 바꾸면 된다.
- 준비 중 SDK도 Maven에 아티팩트가 올라와 있으면 의존성 추가와 컴파일은 될 수 있다(❓ 미확인). 되면 스텁 대신 실제 클래스를 감싸고 런타임 실패 시에만 Fake로 떨어지게 만든다.

## 14. 다시 조사할 때

- **공모전 공지사항**: https://appdevchallenge.auto-hmg.io/notice 를 먼저 확인한다. 이 문서 작성 시점에는 공지 3건이었다.
- **SDK 버전 변경**: `https://document.pleos.ai/en/docs/pleos-only/release-note/`에서 확인한다(사이트맵의 release-note 경로).
- **전체 API 목록**: `https://document.pleos.ai/en/sitemap.xml`에서 `connect-sdk-pleos/` 경로를 거른다. 문서 사이트는 Docusaurus 정적 HTML이라 `curl`로 본문을 읽을 수 있다. WebFetch로는 본문이 비어서 온다.
- **콘솔 상태**: Playground > Workspace > My Project > SDK 메뉴에서 확인한다. 로그인이 필요하다.

---

## 부록 A. 문서 사이트맵에서 추출한 API 목록

- 2026-10-24 `https://document.pleos.ai/en/sitemap.xml` 기준이다.
- URL 경로를 camelCase로 바꾼 것이라 이름은 원문과 같다. 단 `check-v-i-n-info`처럼 약어가 쪼개진 경로는 `checkVINInfo`로 합쳤다.
- 파라미터와 반환 타입은 원문 API Reference에서 확인한다.
- 콘솔 "사용 가능" 목록(§4.3)에 없는 API는 사용 가능 여부가 미확인이다.

### A-1. Vehicle SDK 도메인별 메서드 (문서 사이트맵 기준)

**brake**

- get: `getCurrentBrakePedalStatus`
- register: `registerBrakePedalStatus`
- check: `checkBrakePedalStatusCapabilities`

**car-info**

- get: `getCRNInfo`, `getVINInfo`
- check: `checkCRNInfoCapabilities`, `checkVINInfoCapabilities`

**display**

- get: `getCurrentTemperatureDisplayUnit`
- register: `registerTemperatureDisplayUnit`
- check: `checkTemperatureDisplayUnitCapabilities`

**door**

- get: `getCurrentDoorPosition`, `getCurrentGloveBoxLockState`, `getDoorLockState`
- register: `registerDoorLockState`, `registerDoorPosition`, `registerGloveBoxLockState`
- check: `checkDoorLockCapabilities`, `checkDoorPositionCapabilities`, `checkGloveBoxLockCapabilities`
- set: `setDoorLock`, `setDoorPosition`, `setGloveBoxLock`

**driving-mode**

- get: `getCruiseControlState`, `getCruiseControlTargetSpeed`, `getCurrentDrivingModeState`
- register: `registerCruiseControlState`, `registerCruiseControlTargetSpeed`, `registerDrivingModeState`
- check: `checkCruiseControlStateCapabilities`, `checkCruiseControlTargetSpeedCapabilities`, `checkDrivingModeStateCapabilities`

**ev-battery**

- get: `getCumulativeEvEfficiency`, `getCurrentEvEfficiency`, `getEvBatteryStateOfCharge`, `getEvChargeCurrentDrawLimit`, `getEvChargeSwitchState`, `getInstantaneousEvEfficiency`, `getSinceChargeEvEfficiency`
- register: `registerCumulativeEvEfficiency`, `registerCurrentEvEfficiency`, `registerEvBatteryStateOfCharge`, `registerInstantaneousEvEfficiency`, `registerSinceChargeEvEfficiency`
- check: `checkCumulativeEvEfficiencyCapabilities`, `checkCurrentEvEfficiencyCapabilities`, `checkEvBatteryStateOfChargeCapabilities`, `checkEvChargeCurrentDrawLimitCapabilities`, `checkEvChargePercentLimitCapabilities`, `checkEvChargePortOpenCapabilities`, `checkEvChargeSwitchCapabilities`, `checkInstantaneousEvEfficiencyCapabilities`, `checkSinceChargeEvEfficiencyCapabilities`
- set: `setEvChargeCurrentDrawLimit`, `setEvChargePercentLimit`, `setEvChargePortOpen`, `setEvChargeSwitch`

**fuel**

- get: `getCumulativeFuelEconomy`, `getCurrentFuelEconomy`, `getFuelPercent`, `getInstantaneousFuelEconomy`, `getSinceChargeFuelEconomy`
- register: `registerCumulativeFuelEconomy`, `registerCurrentFuelEconomy`, `registerFuelPercent`, `registerInstantaneousFuelEconomy`, `registerSinceChargeFuelEconomy`
- check: `checkCumulativeFuelEconomyCapabilities`, `checkCurrentFuelEconomyCapabilities`, `checkFuelPercentCapabilities`, `checkInstantaneousFuelEconomyCapabilities`, `checkSinceChargeFuelEconomyCapabilities`

**h-v-a-c**

- get: `getHvacAcState`, `getHvacAirCleanerState`, `getHvacAutoRecirculationState`, `getHvacAutoState`, `getHvacDefrosterState`, `getHvacDualState`, `getHvacFanDirection`, `getHvacFanSpeed`, `getHvacMaxAcState`, `getHvacMaxDefrostState`, `getHvacPowerState`, `getHvacRecirculationState`, `getHvacTemperature`, `getHvacTemperatureCurrent`
- register: `registerHvacAcState`, `registerHvacAirCleanerState`, `registerHvacAutoRecirculationState`, `registerHvacAutoState`, `registerHvacDefrosterState`, `registerHvacDualState`, `registerHvacFanSpeed`, `registerHvacMaxAcState`, `registerHvacMaxDefrostState`, `registerHvacPowerState`, `registerHvacRecirculationState`, `registerHvacTemperature`, `registerHvacTemperatureCurrent`
- check: `checkHvacAcCapabilities`, `checkHvacAirCleanerCapabilities`, `checkHvacAutoCapabilities`, `checkHvacAutoRecirculationCapabilities`, `checkHvacDefrosterCapabilities`, `checkHvacDualCapabilities`, `checkHvacFanDirectionCapabilities`, `checkHvacFanSpeedCapabilities`, `checkHvacMaxAcCapabilities`, `checkHvacMaxDefrostCapabilities`, `checkHvacPowerCapabilities`, `checkHvacRecirculationCapabilities`, `checkHvacTemperatureCapabilities`, `checkHvacTemperatureCurrentCapabilities`
- set: `setHvacAc`, `setHvacAirCleaner`, `setHvacAuto`, `setHvacAutoRecirculation`, `setHvacDefroster`, `setHvacDual`, `setHvacFanDirection`, `setHvacFanSpeed`, `setHvacMaxAc`, `setHvacMaxDefrost`, `setHvacPower`, `setHvacRecirculation`, `setHvacTemperature`

**light**

- get: `getAmbientColorAutoByDriveModeEnabled`, `getAmbientLightBrightness`, `getAmbientLightBrightnessColor`, `getAmbientLightState`, `getAmbientLightSwitch`, `getCabinLightSwitch`, `getCurrentCabinLightState`, `getCurrentFrontFogLightState`, `getCurrentHighBeamLightState`, `getCurrentLowBeamLightState`, `getCurrentReadingLightState`, `getCurrentRearFogLightState`, `getCurrentTailLampLightState`, `getFrontFogLightSwitch`, `getHighBeamLightSwitch`, `getLowBeamLightSwitch`, `getReadingLightSwitch`, `getRearFogLightSwitch`, `getTailLampLightSwitch`
- register: `registerAmbientColorAutoByDriveModeEnabled`, `registerAmbientLightBrightnessColorListener`, `registerAmbientLightBrightnessListener`, `registerAmbientLightStateListener`, `registerCabinLightState`, `registerFrontFogLightState`, `registerHighBeamLightState`, `registerLowBeamLightState`, `registerReadingLightState`, `registerRearFogLightState`, `registerTailLampLightState`
- check: `checkAmbientColorAutoByDriveModeEnabledCapabilities`, `checkAmbientLightBrightnessCapabilities`, `checkAmbientLightBrightnessColorCapabilities`, `checkAmbientLightStateCapabilities`, `checkAmbientLightSwitchCapabilities`, `checkCabinLightStateCapabilities`, `checkCabinLightSwitchCapabilities`, `checkFrontFogLightStateCapabilities`, `checkFrontFogLightSwitchCapabilities`, `checkHighBeamLightStateCapabilities`, `checkHighBeamLightSwitchCapabilities`, `checkLowBeamLightStateCapabilities`, `checkLowBeamLightSwitchCapabilities`, `checkReadingLightStateCapabilities`, `checkReadingLightSwitchCapabilities`, `checkRearFogLightStateCapabilities`, `checkRearFogLightSwitchCapabilities`, `checkTailLampLightStateCapabilities`, `checkTailLampLightSwitchCapabilities`
- set: `setAmbientColorAutoByDriveModeEnabled`, `setAmbientLightBrightness`, `setAmbientLightBrightnessColor`, `setAmbientLightSwitch`, `setCabinLightSwitch`, `setFrontFogLightSwitch`, `setHighBeamLightSwitch`, `setLowBeamLightSwitch`, `setReadingLightSwitch`, `setRearFogLightSwitch`, `setTailLampLightSwitch`

**odometer**

- get: `getCurrentDistanceDriven`, `getTripCumulativeDrivingDistance`, `getTripCumulativeDrivingTime`, `getTripCurrentDrivingDistance`, `getTripCurrentDrivingTime`, `getTripInstantaneousDrivingDistance`, `getTripSinceRefillDrivingDistance`, `getTripSinceRefillDrivingTime`
- register: `registerDistanceDriven`, `registerTripCumulativeDrivingDistance`, `registerTripCumulativeDrivingTime`, `registerTripCurrentDrivingDistance`, `registerTripCurrentDrivingTime`, `registerTripInstantaneousDrivingDistance`, `registerTripSinceRefillDrivingDistance`, `registerTripSinceRefillDrivingTime`
- check: `checkCurrentDistanceDrivenCapabilities`, `checkTripCumulativeDrivingDistanceCapabilities`, `checkTripCumulativeDrivingTimeCapabilities`, `checkTripCurrentDrivingDistanceCapabilities`, `checkTripCurrentDrivingTimeCapabilities`, `checkTripDrivingDistanceCapabilities`, `checkTripDrivingTimeCapabilities`, `checkTripInstantaneousDrivingDistanceCapabilities`, `checkTripSinceRefillDrivingDistanceCapabilities`, `checkTripSinceRefillDrivingTimeCapabilities`
- set: `setRangeRemainingDistance`

**safety**

- get: `getBrakeFluidLevelState`, `getEngineOilLevelState`, `getTelltalesInfo`, `getTransmissionOilLevelState`
- has: `hasLowWasherFluidWarning`, `hasPowerDownWarningIndicator`, `hasSeatAirbagBeenDeployed`, `hasSeatAirbagEnabled`, `hasSeatBeltNotBuckledWarning`
- register: `registerBrakeFluidLevelState`, `registerEngineOilLevelState`, `registerLowWasherFluidWaringState`, `registerPowerDownWarningIndicator`, `registerSeatAirbagDeployed`, `registerSeatAirbagEnabled`, `registerSeatBeltNotBuckledWarning`, `registerTelltales`, `registerTransmissionOilLevelState`
- check: `checkAirbagCapabilities`, `checkAirbagDeployedCapabilities`, `checkBrakeFluidLevelStateCapabilities`, `checkEngineOilLevelStateCapabilities`, `checkLowWasherFluidWarningStateCapabilities`, `checkPowerDownWarningIndicatorCapabilities`, `checkSeatBuckledCapabilities`, `checkSeatNotBuckledWarningCapabilities`, `checkTelltalesCapabilities`, `checkTransmissionOilLevelStateCapabilities`

**seat**

- get: `getCurrentSeatBackrestAnglePosition`, `getCurrentSeatBeltBuckledState`, `getCurrentSeatBeltHeightPosition`, `getCurrentSeatDepthPosition`, `getCurrentSeatForeAftPosition`, `getCurrentSeatHeadrestAnglePosition`, `getCurrentSeatHeadrestForeAftPosition`, `getCurrentSeatHeadrestHeightAftPosition`, `getCurrentSeatHeightPosition`, `getCurrentSeatLumbarForeAftPosition`, `getCurrentSeatLumbarSideSupportPosition`, `getCurrentSeatOccupancy`, `getCurrentSeatTiltPosition`, `getHvacSeatTemperature`, `getHvacSeatVentilationState`
- register: `registerHvacSeatTemperature`, `registerHvacSeatVentilation`, `registerSeatBackrestAnglePosition`, `registerSeatBeltBuckledState`, `registerSeatBeltHeightPosition`, `registerSeatDepthPosition`, `registerSeatForeAftPosition`, `registerSeatHeadrestAnglePosition`, `registerSeatHeadrestForeAftPosition`, `registerSeatHeadrestHeightAftPosition`, `registerSeatHeightPosition`, `registerSeatLumbarForeAftPosition`, `registerSeatLumbarSideSupportPosition`, `registerSeatOccupancy`, `registerSeatTiltPosition`
- check: `checkMoveSeatBackrestAngleCapabilities`, `checkMoveSeatBeltHeightCapabilities`, `checkMoveSeatDepthCapabilities`, `checkMoveSeatForeAftCapabilities`, `checkMoveSeatHeadrestAngleCapabilities`, `checkMoveSeatHeadrestForeAftCapabilities`, `checkMoveSeatHeadrestHeightCapabilities`, `checkMoveSeatHeightCapabilities`, `checkMoveSeatLumbarForeAftCapabilities`, `checkMoveSeatLumbarSideSupportCapabilities`, `checkMoveSeatTiltCapabilities`, `checkSeatBackrestAngleCapabilities`, `checkSeatBackrestAnglePositionCapabilities`, `checkSeatBeltBuckledCapabilities`, `checkSeatBeltHeightCapabilities`, `checkSeatBeltHeightPositionCapabilities`, `checkSeatDepthCapabilities`, `checkSeatDepthPositionCapabilities`, `checkSeatForeAftCapabilities`, `checkSeatForeAftPositionCapabilities`, `checkSeatHeadrestAngleCapabilities`, `checkSeatHeadrestAnglePositionCapabilities`, `checkSeatHeadrestForeAftCapabilities`, `checkSeatHeadrestForeAftPositionCapabilities`, `checkSeatHeadrestHeightCapabilities`, `checkSeatHeadrestHeightPositionCapabilities`, `checkSeatHeightCapabilities`, `checkSeatHeightPositionCapabilities`, `checkSeatLumbarForeAftCapabilities`, `checkSeatLumbarForeAftPositionCapabilities`, `checkSeatLumbarSideSupportCapabilities`, `checkSeatLumbarSideSupportPositionCapabilities`, `checkSeatMemoryCapabilities`, `checkSeatOccupancyCapabilities`, `checkSeatTemperatureCapabilities`, `checkSeatTiltCapabilities`, `checkSeatTiltPositionCapabilities`, `checkSeatVentilationCapabilities`
- set: `setHvacSeatTemperature`, `setHvacSeatVentilation`
- move: `moveSeatBackrestAngle`, `moveSeatBeltHeight`, `moveSeatDepth`, `moveSeatForeAft`, `moveSeatHeadrestAngle`, `moveSeatHeadrestForeAft`, `moveSeatHeadrestHeight`, `moveSeatHeight`, `moveSeatLumbarForeAft`, `moveSeatLumbarSideSupport`, `moveSeatTilt`
- select: `selectSeatMemory`

**side-mirror**

- get: `getHvacSideMirrorHeatState`
- register: `registerHvacSideMirrorHeatState`
- check: `checkHvacSideMirrorHeatCapabilities`
- set: `setHvacSideMirrorHeat`

**steering-wheel**

- get: `getCurrentSteeringWheelAngle`, `getHvacSteeringWheelHeatState`
- register: `registerHvacSteeringWheelHeatState`, `registerSteeringWheelAngle`
- check: `checkHvacSteeringWheelHeatCapabilities`, `checkSteeringWheelAngleCapabilities`
- set: `setHvacSteeringWheelHeat`

**tire**

- get: `getLowTirePressure`
- check: `checkLowTirePressureCapabilities`, `checkWheelAreaCapabilities`

**toll**

- get: `getCurrentEtcsVoiceGuidanceVolume`
- register: `registerEtcsVoiceGuidanceVolume`
- check: `checkEtcsVoiceGuidanceVolumeCapabilities`
- set: `setEtcsVoiceGuidanceVolume`

**turn-signal**

- get: `getCurrentTurnSignalState`
- register: `registerTurnSignalState`
- check: `checkTurnSignalStateCapabilities`

**window**

- get: `getCurrentSmartGlassLevel`, `getCurrentSmartGlassState`, `getCurrentWindowBlindPosition`, `getCurrentWindowLockState`, `getCurrentWindowPosition`, `getSmartGlassConfig`
- register: `registerSmartGlassLevel`, `registerSmartGlassState`, `registerWindowBlindPosition`, `registerWindowLockState`, `registerWindowPosition`
- check: `checkMoveWindowBlindCapabilities`, `checkSmartGlassLevelCapabilities`, `checkSmartGlassStateCapabilities`, `checkSmartGlassZoneStateCapabilities`, `checkWindowBlindPositionCapabilities`, `checkWindowLockCapabilities`, `checkWindowPositionCapabilities`
- set: `setSmartGlassLevel`, `setSmartGlassState`, `setSmartGlassZoneState`, `setWindowLock`, `setWindowPosition`
- move: `moveWindowBlind`

**wiper**

- get: `getCurrentWiperState`, `getCurrentWiperSwitch`
- register: `registerWiperState`
- check: `checkWiperStateCapabilities`, `checkWiperSwitchCapabilities`
- set: `setWiperSwitch`

unregister는 register와 1:1 쌍이라 생략했다.

### A-2. NaviHelper 메서드 (사이트맵 기준)

`addListener`, `addWaypoint`, `cancelRoute`, `changeRouteOption`, `getBookmarkInfo`, `getChargerOperatorInfo`, `getCurrentLocationInfo`, `getDestinationInfo`, `getRecentDestinationInfo`, `getRouteStateInfo`, `getTBTInfo`, `getWaypointInfo`, `initialize`, `release`, `removeListener`, `removeWaypoint`, `requestNavigationScreen`, `requestReRoute`, `requestRoute`, `requestRouteWithWaypoints`, `requestTrip`

### A-3. NaviHelperEventListener 콜백 (사이트맵 기준)

`onBookMarkChanged`, `onBookmarkInfo`, `onCameraAlert`, `onCautionPointInfo`, `onChargerOperatorInfo`, `onCurrentLocationInfo`, `onCurrentRoadSpeedLimit`, `onDestinationArrived`, `onDestinationChanged`, `onDestinationInfo`, `onDrivingInfo`, `onError`, `onNavigationStatus`, `onReRouteEnded`, `onRecentDestinationInfo`, `onRouteCancelled`, `onRouteOptionChanged`, `onRouteStarted`, `onRouteStateInfo`, `onRoutingCompleted`, `onTBTInfo`, `onTripInfo`, `onWayPointArrived`, `onWayPointInfo`, `onWaypointChanged`

### A-4. SpeechToText 패키지별 멤버 (사이트맵 기준)

- `ai.pleos.playground.stt`: `addListener`, `completeAudioSend`, `initialize`, `registerApp`, `release`, `removeListener`, `request`, `sendAudio`, `setLocale`, `speechToText`, `stop`, `version`
- `ai.pleos.playground.stt.listener`: `onConnected`, `onEndedRecognition`, `onError`, `onFailed`, `onFailure`, `onReady`, `onStartedRecognition`, `onSuccess`, `onUpdated`, `onUpdatedEpdData`, `onUpdatedRms`

### A-5. TextToSpeech 패키지별 멤버 (사이트맵 기준)

- `ai.pleos.playground.tts`: `addEventListener`, `initialize`, `registerApp`, `release`, `removeEventListener`, `setAudioAttributes`, `setAudioZoneId`, `setLocale`, `setSpeakerType`, `speak`, `stop`, `textToSpeech`, `version`
- `ai.pleos.playground.tts.listener`: `onConnected`, `onDone`, `onError`, `onFailed`, `onFailure`, `onReady`, `onStart`, `onStop`, `onSuccess`, `onUpdatedRms`

### A-6. FusedLocation 패키지별 멤버 (사이트맵 기준)

- `ai.pleos.playground.location.fused`: `addProximityAlert`, `fusedLocation`, `getLastKnownLocation`, `initialize`, `isSupported`, `registerFusedLocationCallback`, `release`, `removeProximityAlert`, `showLocationAccessBlockedPopup`, `unregisterFusedLocationCallback`
- `ai.pleos.playground.location.fused.listener`: `onChanged`, `onError`, `onRegistered`, `onTransition`
- `ai.pleos.playground.location.fused.model`: `alertId`, `altitude`, `confidence`, `heading`, `latitude`, `locationData`, `longitude`, `proximityAlertEvent`, `proximityAlertRequest`, `radiusMeters`, `timestampMillis`, `transition`, `transitionTypes`

