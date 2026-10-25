# App Design Guide

Pleos Connect 앱을 설계·구현할 때 지켜야 할 세부 규칙이다. 원칙은 [설계 원칙](APP_DESIGN_PRINCIPLES.md), 화면 디자인 규칙은 [디자인 가이드](UI_DESIGN_GUIDE.md)를 본다.

출처는 [Pleos Connect 앱 설계 가이드](https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/intro)의 각 페이지이며, 2026-10-21에 읽은 내용이다. 원문에 수치가 없는 항목은 "원문에 수치 없음"으로 표시했다. 정책은 플랫폼 버전과 양산 차량에 따라 달라질 수 있으므로 구현 전에 원문을 다시 확인한다.

## 1. 주행 안전

출처: [주행 안전을 고려한 설계](https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/driver-distraction-guideline)

[AAOS 운전자 주의 분산 가이드라인](https://source.android.com/docs/automotive/driver_distraction/guidelines?hl=ko)을 준수한다.

### 필수 요구사항

- 주행 중 제한 상태는 `CarUxRestrictions`로 확인한다. 연동 방법은 Pleos의 "주행 규제 개발 가이드"에 있다.
- 제한 대상 기능의 UI와 동작은 축소하거나 비활성화한다.
- 주행 규제 상태에서는 영상 재생을 시작하거나 재개하지 않는다.
- 오디오 재생은 "오디오 포커스 개발 가이드"의 정책을 따른다.
- 게임, 인터넷 서핑, 피트니스 등 주의를 분산시키는 활동을 유도하지 않는다.

### UX 규칙

| 영역 | 규칙 |
|---|---|
| 시선 | 정보는 간결하게 표시한다. 주행 중 동영상, 애니메이션, 움직이는 이미지는 최소화하거나 제한한다. 장식적이거나 반복적으로 움직이는 요소는 쓰지 않는다. |
| 정보 구조 | 자주 쓰는 기능은 첫 화면 상단에 둔다. 드롭다운과 다단 메뉴를 남용하지 않는다. 자동 애니메이션과 반복적인 화면 전환을 줄인다. |
| 터치 | 큰 터치 영역, 높은 대비, 최소한의 텍스트를 쓴다. 버튼 크기와 간격은 [디자인 가이드](UI_DESIGN_GUIDE.md)의 기준을 따른다. 음성 명령과 물리 버튼 입력을 지원한다. |
| 입력 | 검색과 로그인에서도 키보드보다 프리셋, 최근 항목, 음성 입력을 먼저 제공한다. |
| 오디오 | 운전에 중요한 오디오 안내가 나오면 미디어를 일시 중지하거나 볼륨을 줄인다. |

HUD, 클러스터, OSD 영역을 쓰는 경우 해당 영역의 정책을 따른다(원문에 내용 없음).

## 2. 주행 규제

출처: [지역별 주행 규제 정책](https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/driving-restiction-policy)

주행 규제는 속도, 기어, 브레이크 같은 차량 신호와 지역별 안전 규정에 따라 적용된다(속도 임계값은 원문에 수치 없음).

### Distraction Optimization 선언

DO를 선언하지 않은 앱은 Parking에서만 쓸 수 있고 Driving에서는 쓸 수 없다. 주행 중에도 쓰는 앱이면 DO를 선언해야 한다.

이 앱은 Mobility Services 카테고리이고, 주행과 관련된 차량 관리와 주유소 탐색은 주행 중 제한적으로 허용될 수 있다. 아직 주행 중 기능을 만들지 않아서 `MainActivity`에 DO를 선언하지 않았고, 지금은 정차 중에만 쓸 수 있다. 주행 중 기능(경고 표시, 주유소 경유지 제안 등)을 추가할 때 DO를 선언하고, `CarUxRestrictions`를 받아 제한된 기능을 끄는 처리를 함께 구현한다.

### 기능별 대응 플래그

플래그 이름의 `UX_RESTRICTIONS_` 접두사는 생략했다.

| # | 기능 | 대응 플래그 |
|---|---|---|
| ① | 키보드 | `NO_KEYBOARD` |
| ② | 다이얼패드 | `NO_DIALPAD` |
| ③ | 검색(인포테인먼트 통합 검색) | `NO_KEYBOARD`, `NO_FILTERING` |
| ④ | 미디어 앱 로그인, 설정, 검색 | `NO_SETUP`, `NO_KEYBOARD`, `NO_FILTERING` |
| ⑤ | 디지털키, 도어, 시트, 차량 설정 | `NO_SETUP` |
| ⑥ | 동영상 시청 | `NO_VIDEO` |
| ⑦ | 내비게이션 검색 | `NO_KEYBOARD`, `NO_FILTERING` |
| ⑧ | QR 로그인 | `NO_SETUP` |
| ⑨ | 매뉴얼(대용량 텍스트) | `LIMIT_CONTENT`, `LIMIT_STRING_LENGTH` |

### 지역별 Driving 허용 범위

Parking에서는 모든 지역에서 ①–⑨를 모두 쓸 수 있다. 아래는 Driving 상태다(✅ 허용, ⛔ 제한).

| 지역 | ① | ② | ③ | ④ | ⑤ | ⑥ | ⑦ | ⑧ | ⑨ |
|---|---|---|---|---|---|---|---|---|---|
| 북미 | ⛔ | ⛔ | 음성만 | ⛔ | ⛔ | ⛔ | ⛔ | ⛔ | ⛔ |
| 유럽 | ✅ | ✅ | ✅ | ⛔ | ⛔ | ⛔ | ✅ | ⛔ | ⛔ |
| 그 외 | ✅ | ✅ | ✅ | ✅ | ⛔ | ⛔ | ✅ | ⛔ | ⛔ |

설계 시 유의할 점은 다음과 같다.

- 동영상, 차량 설정, QR 로그인, 대용량 텍스트는 모든 지역에서 주행 중 제한된다. 이 기능은 처음부터 정차 중 전용으로 설계한다.
- 북미는 주행 중 검색도 음성으로만 허용된다. 음성 검색 경로를 기본으로 갖춘다.
- ③과 ④는 같은 플래그를 쓰지만 유럽에서는 허용 여부가 다르다. 플래그만으로 구분되지 않으므로 기능 종류에 따라 판단한다.
- InfoLINK용 앱은 지역별 키보드 분기가 필요할 수 있다(문의: partnership@pleos.ai).

원문이 정의하지 않은 부분: Idling 상태의 동작, 각 지역에 포함되는 국가(한국이 "그 외"인지).

## 3. 앱 카테고리

출처: [앱 카테고리](https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/app-category)

카테고리는 설계 시점에 확정한다.

| 카테고리 | 핵심 목적 | 주행 중 제한적 허용 |
|---|---|---|
| Maps/Navigation | 위치 기반 길 안내 | 있음 |
| Mobility Services | 이동 수단·이동 서비스 제공 | 있음 |
| Music/Audio | 오디오 콘텐츠 중심 | 있음 |
| Lifestyle | 일상생활 관리 및 향상 | 원문에 언급 없음 |
| Entertainment | 오락 및 여가 | 원문에 언급 없음 |
| Business | 업무 및 생산성 | 원문에 언급 없음 |
| Kids | 아동 대상 콘텐츠 | 원문에 언급 없음 |
| Social/Communication | 사람 간 소통 | 원문에 언급 없음 |

주행 중 허용될 수 있는 기능의 예시는 다음과 같다. 실제 허용 여부는 주행 상태와 지역 정책에 따른다.

- **Music/Audio:** 앨범 아트 같은 단순 시각 정보, 재생·일시정지·곡 넘기기, 스트리밍 청취, 라디오 채널 변경, 오디오북 재생
- **Maps/Navigation:** 경로 안내와 실시간 교통 정보, 간결한 지도, TBT 음성 안내, 경로 탐색, 빠른 길 안내, 주변 교통 상황
- **Mobility Services:** 주행과 관련된 차량 관리, 현재 위치 기반 정보, 전기차 충전소 찾기와 경유지 설정, 주유소 탐색, 주차장 위치 확인

## 4. 앱 구조와 화면 모델

출처: [앱 구조 및 동작 모델](https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/app-structure-screen-model)

### 화면 구성

- 화면은 운전자 쪽에 고정된 **Driving view**와 **앱 영역**으로 나뉜다. 앱은 Driving view를 가리거나 종료할 수 없다.
- 앱 영역에는 최대 2개의 앱이 표시된다.

| 모드 | window | 설계 방향 |
|---|---|---|
| portrait | 1개 | 핵심 기능과 주요 정보 |
| landscape | 2개(앱 영역 전체) | 확장 영역을 활용한 부가 정보와 확장 기능 |

- `screenOrientation`을 지정하지 않으면 portrait, window 1개로 실행된다. `landscape`로 지정하면 window 2개를 쓴다.
- 새 앱은 현재 표시 중인 앱의 방향을 따른다. landscape 앱이 표시 중이면 landscape로, portrait 앱 2개가 표시 중이면 primary window에 portrait로 실행된다.
- 사용자는 window bar나 제스처로 앱의 위치와 크기를 바꿀 수 있다. 레이아웃은 반응형으로 만든다.

### 생명주기

화면 분할, window 이동, 다른 앱 실행, 주행 상황 변화로 포그라운드·백그라운드 전환이 자주 일어난다. 화면에서 사라진 앱은 종료되지 않고 백그라운드에 남으며, 표시 중인 앱이 종료되면 최근 사용 순으로 다시 표시될 수 있다.

| 시점 | 할 일 |
|---|---|
| 포그라운드 진입 | 상태를 복원하고 현재 크기와 방향에 맞게 UI를 다시 구성한다. |
| 백그라운드 전환 | 화면 갱신, 애니메이션, 불필요한 센서 수집을 멈춘다. |
| 백그라운드 지속 작업 | 오디오, 통신, 다운로드는 작업별로 실행 조건과 중단·재개 정책을 정의한다. |
| 프로세스 재생성 | 사용자 데이터와 진행 상태를 복원할 수 있도록 상태를 저장한다. |

## 5. 하드웨어 접근

출처: [하드웨어 접근 정책](https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/platform-api-architecture)

- 차량의 물리적 제어나 센서에 직접 접근하지 않는다. 차량 상태는 플랫폼 API로 읽고 제어한다.
- Telephony, SMS, 일부 센서(자이로, 근접 등), 캘린더, 주소록은 차량에서 제공되지 않거나 제한될 수 있다. 필수가 아니면 제거하고, 필요하면 지원 여부를 확인한 뒤 동작하는 선택 기능으로 만든다.

### 네트워크

| 전략 | 내용 |
|---|---|
| 캐싱 | 최근 사용한 콘텐츠를 로컬에 저장해 오프라인에서도 보여 준다. |
| 통신 | 재시도 로직과 타임아웃을 둔다(원문에 수치 없음). |
| 피드백 | 연결이 끊기면 오프라인 상태를 알려 주는 UI를 보여 준다. |

## 6. SDK와 API 선택

출처: [SDK API 정책](https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/pleos-connect-sdk-api), [API 호환성 및 가용성](https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/api-compatibility-availability)

### 어떤 API를 쓸지

| 구분 | 접근 경로 | 예시 |
|---|---|---|
| 표준 데이터 | AAOS Car API | 속도, 기어, 외기 온도, 연료 레벨 |
| 특화 기능 | Pleos Connect SDK | 제조사 전용 시트 제어, 특화 라이팅, 고유 보안 인증 |

- AAOS 표준에 같은 기능이 있으면 표준을 쓴다. Pleos Connect SDK는 중복 기능을 제공하지 않는다.
- SDK API가 AAOS 표준으로 편입돼도 기존 SDK API는 최소 2개 메이저 버전 동안 유지된다.

### 지원 여부 확인

| SDK | 호환성 | 확인 방법 |
|---|---|---|
| Vehicle SDK | 차량 모델마다 다름 | 도메인별 `checkXXXCapabilities` |
| NaviHelper SDK | 내비게이션 탑재 차량만 | 내비게이션 탑재 여부 |
| TextToSpeech, SpeechToText SDK | 전체 호환 | 없음 |
| FusedLocation SDK | 차량 모델마다 다름 | `isSupported` |

- AAOS Vehicle Property는 `VehiclePropertyIds`로 ID를 확인하고, `CarPropertyManager`로 실제 차량에서의 지원과 접근 가능 여부를 확인한다.
- 차량 모델명으로 분기하지 않는다.
- SDK가 제공하지 않는 특수 프로퍼티가 필요하면 직접 구현하기 전에 Pleos 기술 지원팀에 검토를 요청한다.

## 7. 보안과 개인정보

출처: [보안 및 개인정보 보호를 고려한 설계](https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/security-privacy). Pleos App Market 등록 기준은 Pleos의 "앱 보안 가이드"를 따로 확인한다.

| 영역 | 규칙 |
|---|---|
| 계정·세션 | 소유자나 운전자가 바뀌면 쉽게 로그아웃하고 계정을 전환할 수 있어야 한다. 소셜 로그인, OTT 계정 토큰은 Keystore 같은 안전한 저장소에 둔다. |
| 수집 | 위치, 음성, 계정 정보를 요청할 때 이유를 설명한다. 동의 흐름은 간단하게 만든다. 불필요한 개인정보는 저장하지 않는다. |
| 로그 | 로그에 개인정보가 들어가면 마스킹한다. |
| 외부 전송 | 차량 ID와 사용자 ID의 익명화·가명화 정책을 정한다. 주행 중 행동 데이터는 마케팅 목적으로 쓰지 않는다. |

## 8. 성능과 리소스

출처: [성능 및 리소스 효율을 고려한 설계](https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/performance-resource-mgmt)

| 항목 | 기준 |
|---|---|
| CPU, 내비게이션 앱, 주행 중 | 15% 이하 |
| CPU, 그 외 앱, 주행 중 | 5% 미만 |
| CPU, 모든 앱, 정차 중 | 25% 이하 |
| RAM | 400 MB 이하 |

CPU 측정 기준(코어, 평균·최대)과 RAM 측정 기준(PSS, heap)은 원문에 없다.

- **시작:** Cold start 시 스플래시에 오래 머물지 않도록 필요한 것만 초기화하고 나머지는 지연 로딩한다(시간 기준은 원문에 수치 없음).
- **상주:** 백그라운드의 긴 작업, 불필요한 타이머와 폴링을 줄인다. 이미지와 리스트는 재활용 뷰와 캐시로 메모리를 아낀다.
- **오디오:** Pleos Connect 오디오 포커스 정책을 따르고, 포커스 획득·상실에 대응하는 로직을 반드시 구현한다.

## 9. 프로젝트 구성

[Pleos Connect 앱 개발 시작하기](https://document.pleos.ai/docs/pleos-only/getting-started)와 Vehicle SDK 샘플 앱에 맞춰 다음과 같이 구성했다.

| 항목 | 설정 | 근거 |
|---|---|---|
| 모듈 | `automotive` 하나 | Pleos Connect는 차량 내장 앱이다. Android Auto용 `mobile`과 Car App Library용 `shared`는 삭제했다. |
| 화면 | Compose 기반 일반 Activity(`MainActivity`) | 멀티 윈도우 가이드는 Activity의 `screenOrientation`과 반응형 레이아웃을 전제로 한다. |
| `minSdk` | 28 | 시작 가이드의 최소 요구사항이며, App Market 배포 시 signing key rotation에 필요하다. Pleos Connect Emulator는 API 34다. |
| `screenOrientation` | 지정하지 않음 | portrait, window 1개로 실행된다. 사용자가 window를 landscape로 바꿀 수 있고, 디자인 가이드는 모든 앱이 두 방향을 모두 지원하도록 요구한다. |
| 테마 | Pleos 색상 토큰과 글자 크기 | `ui/theme`의 색상과 타이포그래피를 [디자인 가이드](UI_DESIGN_GUIDE.md) 3·4절 값으로 맞췄다. 시스템의 동적 색상은 쓰지 않는다. |
| 카테고리 | Mobility Services | 2026-10-25에 Lifestyle에서 바꿨다. Android 매니페스트의 `appCategory`에는 이에 해당하는 값이 없어 지정하지 않았다. 카테고리는 App Market 등록 시 정하는 것으로 보이며, 원문에서 확인되지 않았다. |
| DO | 선언하지 않음 | 2절 참고 |

### 아직 하지 않은 것

- **Vehicle SDK 연동:** 차량 데이터를 쓰려면 `app/settings.gradle.kts`에 Pleos Maven 저장소(`https://nexus-playground.pleos.ai/repository/maven-releases/`)를, `app/automotive/build.gradle.kts`에 `ai.pleos.playground:Vehicle`을 추가하고, 필요한 `pleos.car.permission.*` 권한을 선언한다. 저장소 접근 권한이 필요하다.
- **화면 설계:** `MainActivity`는 템플릿의 기본 화면이다. portrait와 landscape 레이아웃, 상태 저장과 복원은 기능을 만들면서 구현한다.
- **폰트:** Pleos 기본 폰트(Asta Sans)는 포함하지 않았다. 시스템 기본 폰트를 쓴다.
