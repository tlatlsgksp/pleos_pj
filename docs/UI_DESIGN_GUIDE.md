# UI Design Guide

Pleos Connect 앱 화면을 디자인하고 구현할 때 따르는 규칙이다. 앱 구조와 동작 규칙은 [설계 가이드](APP_DESIGN_GUIDE.md), 상위 원칙은 [설계 원칙](APP_DESIGN_PRINCIPLES.md)을 본다.

출처는 [Pleos Connect UI Design Guide](https://document.pleos.ai/docs/connect/guide/docs-design/Introduction/intro)이며, 2026-10-21에 읽은 내용이다. 원문의 수치 대부분은 이미지 안에 있고 단위가 적혀 있지 않다. 이 문서의 크기 값은 별도 표기가 없으면 dp로 읽는다. 원문에 없는 값은 "원문에 없음"으로 표시했다. 디자인 에셋(Figma)은 원문의 "Pleos Connect design assets"에서 받는다.

## 1. 디자인 원칙

| 원칙 | 규칙 |
|---|---|
| Easy & Safe | 주행 중 동영상, 애니메이션 이미지, 텍스트 자동 스크롤로 시선을 끌지 않는다. 한 손으로 단순하게 조작할 수 있어야 하며 양손 조작을 요구하지 않는다. 운전자의 손이나 발을 움직이게 하지 않는다. |
| Consistency | 아이콘, 용어, 상호작용 패턴을 차량 기본 UI와 맞춘다. |
| Glanceability | 핵심 정보는 1~2초 안에 파악할 수 있게 보여 준다. 입력 후 0.25초 안에 응답하고, 더 걸리면 진행 중 상태를 보여 준다. |

### 가독성과 대비

- 아이콘, 텍스트, 이미지의 대비는 **4.5:1 이상**이다.
- 중요한 정보는 강한 대비로 표현하고, 색을 많이 쓰지 않는다.
- 아이콘은 국제적으로 통용되는 것을 쓰고 **텍스트 라벨과 함께** 둔다.
- 터치하면 햅틱이나 음성으로 피드백한다.

### 텍스트 길이

| 대상 | 한도 |
|---|---|
| 한 메시지(영어) | 120자 또는 24단어 |
| 한 메시지(일본어) | 30자(영문 기준) |
| 한 줄 | 80자(영문 기준) |
| 문장 또는 단락 | 3줄 |
| 중요 정보 글자 높이 | 5mm 이상 |
| 보조 정보 글자 높이 | 3mm 이상 |

한국어 기준은 원문에 없다.

### 알림

운전자에게 필요한 경우에만 간결하게 보여 준다. 알림으로 광고하지 않으며, 광고주나 제품 이름 외의 텍스트 광고도 금지다.

### 그 밖의 요구사항

- 조명 환경에 따라 Light와 Dark 테마가 자동으로 바뀐다(차량 설정의 Screen Mode Auto). 두 테마를 모두 지원한다.
- 음성 비서 Gleo AI가 있으므로 음성 조작을 검토한다.
- 다국어 UI로 설계하고, 여러 차종의 해상도와 화면 비율에 대응한다.

## 2. 화면 구성

출처: Display Layout, Window Policy

### 화면 영역

| 영역 | 내용 | 앱과의 관계 |
|---|---|---|
| Status bar | 주행 상태, Gleo AI와 검색, 시계, 네트워크, 알림 센터, 프로필 | 시스템 UI |
| Driving view | Telltale, 속도, 차량 상태 | 앱이 가리거나 닫을 수 없다. |
| GNB | App library, 즐겨찾는 앱, 공조 | 시스템 UI |
| Application window | 앱 1개 또는 2개 | 앱이 그리는 영역 |

Status bar, GNB 패널, 키보드는 앱보다 위 레이어라서 앱 화면을 잠시 가릴 수 있다.

**모든 앱은 Landscape와 Portrait 레이아웃을 모두 지원해야 한다.** 예외는 가로 전용 게임처럼 한 모드만 쓰는 앱이다.

### 앱 window 크기

| 디스플레이 | 비율 | 화면 해상도 | DPI | Landscape 앱 | Portrait 앱 |
|---|---|---|---|---|---|
| Connect-S-17 (기본) | 16:9 | 2560×1440 | 160 | 1700×1184 | 840×1184 |
| Connect-S-14.6 | 16:9 | 2048×1152 | 200 | 1360×948 | 672×948 |
| Connect-L-12.9 | 16:9 | 1920×1080 | 160 | 1274×855 | 628×855 |
| Connect-W-Standard | 9:2 | 3840×850 | 160 | 1640×810 | 800×810 |
| Connect-W-Extended | 24:9 | 3840×1440 | 160 | 1640×1400 | 800×1400 |
| Connect-WF | 32:9 | 3072×816 | 200 | 1256×800 | 648×800 |

설계 시 유의할 점은 다음과 같다.

- 가장 작은 Portrait 폭은 약 628이고, 가장 낮은 높이는 약 800이다. 핵심 화면은 이 크기에서 깨지지 않아야 한다.
- Connect-W와 WF는 높이가 낮고 가로가 길다. 세로 스크롤에 기대는 화면은 이 디스플레이에서 정보가 적게 보인다.

### Window 동작

| 항목 | Connect-S | Connect-W |
|---|---|---|
| 분할 | 앱 2개면 1:1, 1개면 2:0 | 2:1 또는 1:2 |
| 새 앱 실행 | 기존 앱의 방향을 따른다. | 항상 landscape로 primary window에 연다. 기존 primary는 secondary로 가고 기존 secondary는 사라진다. |
| 위치·크기 변경 | 3손가락 스와이프, window bar 탭·드래그, 앱 window 버튼 | 같음 |
| 종료 | 3손가락 스와이프. 최근 앱이 종료된 창의 방향으로 다시 표시된다. | 같음 |

- Connect-W는 조수석에도 App library가 있고, 조수석에서 실행한 앱은 조수석 window에서만 열린다.
- 다른 앱을 deep link로 열면 새 앱이 기존 앱을 덮고, 끝나면 기존 앱으로 돌아간다.

## 3. 색상

출처: Colors, Design Token, Theme

색은 토큰으로만 쓴다. 토큰 이름의 prefix는 `ivi`이고 snake_case이며 축약하지 않는다. 형식은 `{prefix}_{page}_{component}_{element}_{value}`다(예: `ivi_vehicle_settings_side_menu_normal`). 테마는 브랜드(42dot, Kia, Genesis)와 Light/Dark에 따라 바뀐다.

### Grayscale

Light와 Dark에서 값이 뒤집힌다.

| 토큰 | Light | Dark |
|---|---|---|
| basic_00 | #FFFFFF | #131417 |
| basic_50 | #F7F8FA | #1D1E21 |
| basic_100 | #EDEEF2 | #313236 |
| basic_200 | #E0E1E6 | #44464E |
| basic_300 | #C7C9CE | #686A72 |
| basic_400 | #A4A8AE | #A4A8AE |
| basic_500 | #686A72 | #C7C9CE |
| basic_600 | #44464E | #E0E1E6 |
| basic_700 | #313236 | #EDEEF2 |
| basic_800 | #1D1E21 | #F7F8FA |
| basic_900 | #131417 | #FFFFFF |

### Alpha와 Static

| 토큰 | 값 |
|---|---|
| basic_alpha_light_50~500 | Light #131417, Dark #FFFFFF / 투명도 5, 10, 20, 30, 40, 50% |
| basic_alpha_dark_50~500 | Light #FFFFFF, Dark #131417 / 투명도 5, 10, 20, 30, 40, 50% |
| basic_static_light_100~400 | #FFFFFF 100, 84, 64, 32% (테마 무관) |
| basic_static_dark_100~400 | #131417 100, 84, 64, 32% (테마 무관) |

원문의 alpha 토큰 이름과 실제 색이 직관과 반대이므로 이름이 아니라 값을 기준으로 쓴다.

### 앱에서 자주 쓰는 시스템 토큰

| 용도 | 토큰 | Light | Dark |
|---|---|---|---|
| 배경 | background_primary | basic_50 | basic_50 |
| 배경 | background_secondary | basic_00 | basic_00 |
| 팝업 배경 | background_popup | basic_00 | basic_100 |
| 표면 | surface_basic / low / high / accent | basic_00 / 50 / 100 / 200 | basic_50 / 00 / 100 / 200 |
| 텍스트 | text_dark_primary~quaternary | static_dark_100~400 | static_light_100~400 |
| 아이콘 | icon_dark_primary~quaternary | static_dark_100~400 | static_light_100~400 |
| 기본 버튼 | button_basic_enabled / pressed / disabled | alpha_dark_50 / 100 / alpha_light_200 | 같음 |
| 강조 버튼 | button_filled_enabled / pressed / disabled | basic_600 / 700 / alpha_dark_50 | basic_600 / 500 / alpha_dark_50 |
| 입력 필드 | field_background / field_focused | basic_00 / basic_700 | 같음 |
| 오버레이 | overlay_default | alpha_dark_200 | alpha_light_500 |

### 의미 색

| 토큰 | Light | Dark | 쓰임 |
|---|---|---|---|
| informative_active | #02C265 | #00E074 | 활성 상태, 스위치 On, 체크박스 |
| informative_positive | #0064FF | #0082FF | 긍정, 진행 중 |
| informative_negative | #FE3D16 | #FF4C28 | 오류, 위험 |
| regulation_yellow | #FFC224 | #FFC224 | 법규 표시색 |
| regulation_orange | #FF8A00 | #FF8A00 | 법규 표시색 |
| regulation_red | #F62E24 | #FF4339 | 법규 표시색 |

regulation 계열은 telltale 같은 법규 표시에 쓰이는 색이다. 일반 앱 UI에서 경고 의미로 혼용하지 않는다.

## 4. 타이포그래피

출처: Typography

- 기본 폰트는 Asta Sans이며 한국어와 영어를 지원한다. 브랜드별로 폰트가 바뀔 수 있으며, 개성이 강한 폰트는 쓰지 않는다.
- 최소 크기는 12sp, 실제 화면 높이는 18px 이상을 권장한다.
- 같은 위계의 정보는 화면 전체에서 같은 크기를 쓴다.

| 분류 | 크기 | Size / Line height | Weight |
|---|---|---|---|
| Headline | Large | 60 / 80 | Extra Bold, Bold |
| Headline | Medium | 56 / 72 | Extra Bold, Bold |
| Headline | Small | 48 / 64 | Extra Bold, Bold |
| Title | Large | 40 / 52 | Extra Bold, Bold, Regular |
| Title | Medium | 36 / 44 | Extra Bold, Bold, Regular |
| Title | Small | 32 / 40 | Extra Bold, Bold, Regular |
| Body | Large | 30 / 38 | Strong, Normal |
| Body | Medium | 28 / 36 | Strong, Normal |
| Body | Small | 26 / 34 | Strong, Normal |
| Label | Medium | 24 / 28 | Normal |
| Label | Small | 20 / 24 | Normal |

모바일보다 훨씬 크다. 본문의 최소 크기는 26이다.

## 5. 아이콘

출처: Iconography, App Icon

### UI 아이콘

| 항목 | 규칙 |
|---|---|
| 그리드 | 48×48 |
| 스타일 | Filled와 Outlined를 상황에 따라 쓴다. |
| 선 | stroke 3.6px, round join, round cap |
| 모서리 | 반경 1.5px |
| 크기 | 한 벌을 만들어 필요한 크기로 조정한다. |

### 앱 아이콘

- Android Adaptive Icon 형식으로 만들고, foreground와 background 레이어를 나눈다.
- 시스템이 브랜드별 마스크를 씌운다. Hyundai는 스쿼클, Kia는 사선 컷, Genesis는 작은 라운드 사각이다. 어떤 마스크에서도 로고가 잘리지 않게 안전 영역 안에 둔다.
- GNB, App library, App Market에 쓰인다. App library에서 앱 이름은 2줄까지 보이고 넘치면 말줄임된다.

## 6. 그림자

| Level | 쓰임 | 값 |
|---|---|---|
| 1 | 앱 window | Y6, Blur 24, #000000 8% |
| 2 | 선택되어 떠오른 카드 | Y8, Blur 32, 16% + Y2, Blur 2, 8% |
| 3 | 알림 카드, 최상위 요소 | Y20, Blur 40, 24% + Y2, Blur 4, 20% |

## 7. 모션과 인터랙션

출처: Interaction, Animation

애니메이션은 상태를 설명할 때만 쓴다. 장식용 애니메이션은 쓰지 않는다.

### Easing과 시간

기본 easing은 `cubic-bezier(0.16, 1, 0.3, 1)`이다.

| 토큰 | 시간 | 쓰임 |
|---|---|---|
| ivi_motion_duration_short1 / short2 | 100 / 150ms | 작은 요소 전환 |
| ivi_motion_duration_medium1 / medium2 | 250 / 300ms | 화면 일부를 가로지르는 전환 |
| ivi_motion_duration_long1 / long2 | 400 / 450ms | 큰 전환 |

### 터치 반응

- 누르면 **95%로 줄어들고**(중심 기준) 배경색이 바뀐다. 배경이 없는 버튼에도 눌림 배경을 넣는다.
- 영역 안에서 손을 떼야 실행되고, 손가락이 영역 밖으로 나가면 취소된다.
- 길게 누르기(2~3초)는 편집 모드 진입에 쓴다.
- 스크롤 끝에서는 bounce 효과를 주며, 최대 overscroll은 스크롤 영역의 약 2/5다.

### 로딩

| 종류 | 쓰임 |
|---|---|
| Linear | 짧은 로딩 |
| Tension | 페이지 로딩처럼 시간이 걸리는 경우 |

로딩 에셋은 원문에서 받는다.

## 8. 컴포넌트

출처: Components 하위 페이지. 대부분 컴포넌트의 Large/Medium/Small 실제 크기와 최소 터치 영역은 원문에 없으므로 Figma 에셋에서 확인한다.

### 버튼

| 컴포넌트 | 쓰임 | 크기 | 상태와 모양 |
|---|---|---|---|
| Basic button | 기본 행동 | L / M / S | Primary는 연회색 채움, Secondary는 진한 채움에 흰 글자. Pressed는 더 진하게, Disabled는 아웃라인만, Loading은 라벨 대신 스피너 |
| Toggle button | 여러 옵션 중 On/Off | L / M / S | 선택되면 흰색에 그림자. Floating 속성은 배경과 구분이 필요할 때 쓴다. |
| Text button | 부가 정보 링크 | M / S | 파란 라벨에 ">", 누르면 밑줄 |
| Icon button | 누구나 아는 아이콘일 때만 | L / M / S | Basic(배경), Plain(눌렀을 때만 배경), Circle |

### 입력

| 컴포넌트 | 규칙 |
|---|---|
| Text Field | pill 모양, M / S. Type은 Text, Tel, Url, Email, Password다. Focused는 진한 테두리, 입력 중에는 지우기(x) 버튼, 오류는 빨간 테두리에 아래 안내 문구, 읽기 전용은 회색 채움이다. 입력이 끝나면 넘치는 글자는 끝에서 말줄임한다. 비밀번호는 ●로 가리고 보기(eye) 버튼을 둔다. |
| Number Field | PIN 같은 칸 입력, L / M. 상태는 Text Field와 같다. |
| Keypad | 키는 Primary(흰색 문자·숫자), Secondary(회색 전환 키), Special(Shift 등)로 나뉜다. 키보드에는 음성 입력 키가 있다. |
| Dropdown | 메뉴 이동이나 속성 변경, M / S. 선택 항목은 회색 배경에 체크 표시다. 항목 끝에 Switch를 넣어 일반 기능 메뉴로도 쓴다. |

주행 중에는 키보드 입력이 지역에 따라 제한된다([설계 가이드](APP_DESIGN_GUIDE.md) 2절). 입력 화면에는 프리셋, 최근 항목, 음성 입력을 함께 둔다.

### 선택

| 컴포넌트 | 모양 |
|---|---|
| Checkbox | 라운드 사각. 체크되면 녹색(informative_active) 채움에 흰 체크. 일부 선택(—) 상태가 있다. |
| Radio button | 선택되면 녹색 두꺼운 링 |
| Switch | 라운드 사각 트랙과 흰 손잡이. Off는 회색에 손잡이 왼쪽, On은 녹색에 손잡이 오른쪽 |
| Tabs | Box형(선택 항목이 흰 칩에 그림자)과 Line형(선택 탭 밑줄). Fitted이면 균등 폭, 아니면 왼쪽 정렬 |
| Spinner | 날짜·시간 선택. 가운데 행 강조, 약 5행 표시. Date 폭 568(바깥 여백 40, 열 간격 24), Time 폭 468, 높이 384 |

### 조절

| 컴포넌트 | 규칙 |
|---|---|
| Continuous Slider | 볼륨 같은 연속 값. 양옆에 ±버튼을 둘 수 있고, 손잡이, 트랙, 버튼 모두 터치 영역이다. Basic과 Floating(밝은 배경용)이 있다. |
| Centered Slider | 온도처럼 가운데 기준이 있는 값. 누르면 손잡이가 커진다. |
| Stepper | 전체 400×100, 가운데 값 영역 폭 112. 최솟값이나 최댓값에서 해당 버튼이 비활성화된다. |
| Indicator | 페이지 위치 점. Wide와 Narrow가 있다. |

### 탐색과 피드백

| 컴포넌트 | 규칙 |
|---|---|
| Page Navigation | 앱 상단 바. Main형은 앱 아이콘, 제목 드롭다운, 보조 메뉴, 오른쪽 아이콘 메뉴로 구성된다. Depth형은 뒤로 가기 버튼과 제목이며, 부제를 쓰면 오른쪽 메뉴는 없다. |
| System Popup | 폭 Large 1200, Medium 760. 제목과 설명은 가운데 정렬. **확인 버튼이 왼쪽**(진한 채움), 취소가 오른쪽이다. 버튼 배치는 Single, Vertical(확인이 위), Horizontal이다. 뒤 배경은 dim 처리한다. |
| Toast | 앱 window 하단 중앙, 좌우·하단 여백 24. 최대 폭 792(텍스트 712, 좌우 패딩 40, 상하 패딩 24). 진한 배경에 흰 글자, 가운데 정렬, 최대 2줄 |
| System Notification | OS 알림. 흰 카드에 그림자 Level 3. 유형은 앱 아이콘, 프로필, 이미지, 액션이다. |
| Scroll bar | Enabled는 gray-alpha-200, 누르면 gray-900 |

## 9. 시스템 영역에 관한 참고

앱이 직접 그리지 않지만 앱 화면과 겹치거나 앱이 연동되는 영역이다.

- **GNB:** 사용자가 앱을 즐겨찾기 슬롯(최대 3개)에 등록할 수 있다. 최근 앱 2개가 표시된다.
- **OSD:** 스티어링 휠 버튼으로 미디어나 차량을 조작하면 GNB 근처에 상태 패널이 2초 동안 뜬다.
- **Widgets:** Driving view 하단에 표시되는 위젯 영역이 있다. 앱이 위젯을 제공할 수 있는지는 원문에 없다.
- **Gleo AI:** 대화 패널이 앱 화면을 잠시 가릴 수 있다. 후진 중이나 통화 중에는 대화할 수 없다는 토스트가 3초 동안 뜬다. 조그 다이얼(CCP) 조작도 지원한다.
- **Driving view:** 주행 중에는 landscape로 바꿀 수 없다.

## 10. 원문의 오류와 확인이 필요한 점

- 최소 터치 영역 크기, 레이아웃 그리드(열, 간격, 여백), 모서리 반경, 대부분 컴포넌트의 실제 크기는 원문에 없다. Figma 에셋에서 확인한다.
- 이미지 속 수치에 단위가 없다. 해상도 표는 dp로 표기되어 있고, 나머지도 dp로 읽었다.
- Icon과 Text 토큰 표에서 light 계열 행이 dark 이름으로 중복 표기되어 있다. 이 문서에서는 dark 계열만 옮겼다.
- 그 밖의 오류: `charging_deep` Dark 값이 다른 값과 어울리지 않는다. Media 토큰의 8자리 hex 형식이 섞여 있다. Stepper의 증감 라벨이 뒤바뀌어 있다. 여러 버튼의 Pressed 설명이 다른 컴포넌트 설명을 복사한 것으로 보인다.
- "1.5배 이상"이 행간을 뜻하는지 크기를 뜻하는지 원문에 없다.
