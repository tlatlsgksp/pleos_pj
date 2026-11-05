# 기능 정리와 로직 기초 기획

기획서 v1.0의 기능을 구현 단위로 묶고, 각 묶음의 데이터와 판단 로직을 정한다. 앱 개발은 이 문서를 기준으로 시작한다.

- 작성일: 2026-10-28
- 근거: [기획서](PROPOSAL.md), [아키텍처](ARCHITECTURE.md), [기획 기반 정리](PLANNING_FOUNDATION.md), [조사 노트 §11](PLEOS_PLATFORM_RESEARCH.md)(에뮬레이터 실측)
- 표시: `[기본값]` 처음 구현할 때 쓸 값. 실주행이나 시연을 보고 조정한다. `[결정 필요]` 팀이 정할 것.

## 0. 범위 결정 (2026-10-28)

| 상태 | 기능 |
|---|---|
| 지금 뺌 | 시동 브리핑, 차량 진단 기반 정비 권유 |
| 나중에 확장 | 법인 운행일지 |
| 보류 | 음성 기록(Gleo 외부 앱 등록 가능 여부 확인 후) |

## 1. 기능 묶음

남은 기능 18개를 **5개 모듈**로 묶는다. 묶는 기준은 "같은 데이터를 만들거나 같은 데이터를 읽는가"다.

| 모듈 | 묶은 기능 | 한 문장 정의 | 우선순위 |
|---|---|---|---|
| **M1. 기록(Ledger)** | 지출 수동 기록, 정비 기록, 주유·충전 기록, 관리 이력서 데이터, 사고 기록, 대여 기간, 음성 기록(입력 경로) | 돈이나 차 상태가 바뀐 사건을 **기록 하나**로 남긴다 | P0 |
| **M2. 주행(Drive)** | 주행거리 자동 기록, 연비·전비, 공회전 분리, 운행 기록·통행료, 효율 원인 분석, 주행 다시보기 | 시동부터 정차까지를 **주행 세션 하나**로 잘라 측정한다 | P0(세션·효율), P1(원인·다시보기) |
| **M3. 관리항목(Care)** | 관리항목 주기 진행률, 정비 기록에 따른 초기화 | M1·M2 값을 읽어 **교체 시점**을 계산한다 | P0 |
| **M4. 경유(Route)** | 진행 방향 기준 주유소·충전소 경유, 정비소 경유 제안, 정비소 방문 자동 기록 | 필요한 곳을 **가는 길에 끼워 넣고**, 다녀오면 M1 초안을 만든다 | P1 |
| **M5. 공유(Share)** | 관리 이력서 화면·공유, 같은 차종 통계·순위, 커뮤니티 프로필, 워키토키 | 쌓인 기록을 **밖으로 보여 준다** | P1(이력서·순위), 후순위(프로필·워키토키) |

합친 이유:
- **사고 기록, 대여 모드 → M1**: 사고는 "에어백이 만든 기록 한 건"이고, 대여 모드는 "기간 태그가 붙은 기록 보기"다. 별도 기능으로 둘 이유가 없다.
- **음성 기록 → M1의 입력 경로**: 음성은 기록을 만드는 또 하나의 방법일 뿐이다. Gleo가 열리면 입력 경로만 추가한다.
- **운행 기록·다시보기·원인 분석 → M2**: 셋 다 주행 세션에 붙는 정보다. 운행 기록은 세션의 요약, 원인 분석은 세션 샘플의 해석, 다시보기는 세션 샘플의 재생이다.
- **정비소 경유와 방문 기록 → M4**: 정비소에 보내는 것과 다녀왔는지 확인하는 것은 한 흐름이다.

모듈 사이의 데이터 흐름:

```
 차량 신호 ──▶ M2 주행 세션 ──┬──▶ M3 관리항목 진행률 ──▶ M4 경유 제안
                              │          ▲                     │
                              ▼          │ 정비 기록           │ 도착·출발
 사용자·감지 ──────────────▶ M1 기록 ────┘ ◀───────────────────┘ (초안)
                              │
                              ▼
                         M5 이력서·순위
```

## 2. 공통 데이터 모델

`core/domain`에 둔다. Room 엔티티는 `core/data`에서 이 모델과 1:1로 매핑한다.

### 2.1 차량

```kotlin
enum class EnergyType { FUEL, EV }          // 하이브리드는 FUEL로 시작 [결정 필요]

data class Vehicle(
    val id: String,                          // 앱이 만든 UUID. VIN을 키로 쓰지 않는다
    val energyType: EnergyType,
    val vinHash: String?,                    // 서버·이력서에는 해시만
    val tankCapacityL: Double?,              // FUEL. 차량 값 → 사용자 입력 순
    val batteryCapacityKwh: Double?,         // EV. 차량 값(실측 77.4kWh)
    val baselineOdometerKm: Double,          // 앱을 처음 켠 시점의 총 주행거리
    val baselineAt: Instant,
)
```

### 2.2 차량 확인값(VehicleStamp): 관리 이력서의 근거

모든 기록과 세션에 붙는다. **차량에서 읽은 값만** 들어가고, 사용자는 고칠 수 없다.

```kotlin
data class VehicleStamp(
    val odometerKm: Double,
    val vinHash: String?,
    val capturedAt: Instant,
    val source: StampSource,                 // VEHICLE(SDK에서 읽음) / DEMO(시나리오) / MANUAL(차량 값 없음)
)
```

- 저장할 때 `odometerKm + vinHash + capturedAt + 기록 핵심 필드`로 해시를 만들어 함께 둔다. 사용자가 금액·메모를 고치면 `editedAt`이 남고, 이력서에 "수정됨"으로 표시한다.
- 서명 방식(기기 키 서명인지, 서버 원본 보관인지)은 M5 공유를 만들 때 정한다 `[결정 필요]`. 지금은 **열과 해시만 처음부터 둔다**. 나중에 소급할 수 없기 때문이다.

### 2.3 기록(Record): M1의 단일 엔티티

주유·충전·정비·지출·사고를 테이블 하나에 담고, 종류별 세부 정보만 나눈다. 통계와 이력서를 조회 한 번으로 만들기 위해서다.

```kotlin
data class Record(
    val id: String,
    val vehicleId: String,
    val type: RecordType,
    val occurredAt: Instant,
    val amountWon: Long?,                    // 지출 금액. 사고 등은 null
    val stamp: VehicleStamp,
    val origin: RecordOrigin,                // MANUAL / DETECTED / VOICE / NAV_VISIT
    val status: RecordStatus,                // DRAFT(감지됨, 확인 전) / CONFIRMED / DISMISSED
    val detail: RecordDetail,
    val memo: String?,
    val rentalPeriodId: String?,             // 대여 기간 중이면 연결
    val editedAt: Instant?,
)

enum class RecordType { ENERGY, MAINTENANCE, EXPENSE, ACCIDENT }

sealed interface RecordDetail {
    data class Energy(val kind: EnergyType, val amount: Double,        // L 또는 kWh
                      val unitPriceWon: Double?, val placeName: String?,
                      val levelBefore: Double?, val levelAfter: Double?) : RecordDetail
    data class Maintenance(val itemIds: List<String>, val shopName: String?) : RecordDetail
    data class Expense(val category: ExpenseCategory) : RecordDetail   // 세차·주차·통행료·보험·세금·기타·사용자 분류
    data class Accident(val speedKmh: Double?, val lat: Double?, val lng: Double?) : RecordDetail
}
```

### 2.4 주행 세션(DriveSession): M2의 단일 엔티티

```kotlin
data class DriveSession(
    val id: String,
    val vehicleId: String,
    val start: VehicleStamp, val end: VehicleStamp?,      // end == null이면 진행 중
    val distanceKm: Double,
    val energyUsed: Double?,                              // kWh 또는 L
    val idleSeconds: Long, val idleEnergyUsed: Double?,
    val tollWon: Long?,                                   // 내비 예상 통행료
    val destinationName: String?,
    val kind: SessionKind,                                // MEASURED(앱이 켜져 있었음) / GAP(꺼져 있던 기간을 복원)
)
```

샘플(시각, 차속, 기어, 에너지, 에어컨, 브레이크, 위치)은 `DriveSample` 테이블에 따로 둔다. 원인 분석과 다시보기가 읽는다. 저장 주기는 `[기본값]` 5초, 위치 갱신 주기를 실측한 뒤 조정한다.

### 2.5 관리항목

```kotlin
data class CareItemDef(                                   // 앱에 내장하는 기준표
    val id: String, val name: String,
    val energyTypes: Set<EnergyType>,                     // 엔진오일 = FUEL만, 감속기오일 = EV만
    val intervalKm: Int?, val intervalMonths: Int?,
)

data class CareItemState(                                 // 차량마다
    val vehicleId: String, val itemId: String,
    val lastServiceOdometerKm: Double?, val lastServiceAt: Instant?,
    val customIntervalKm: Int?, val customIntervalMonths: Int?,   // 사용자 조정
    val lastRecordId: String?,                            // 이 상태를 만든 정비 기록. 기록 삭제 시 되돌리기용
)
```

기준표의 주기 값은 제조사 정비 매뉴얼 일반 조건 기준으로 채우고 출처를 단다([기획 기반 정리 §2.3](PLANNING_FOUNDATION.md)).

## 3. 모듈별 로직

### M1. 기록

**L1-1. 기록 저장 규칙**
1. 저장 직전에 차량에서 총 주행거리·VIN을 읽어 `VehicleStamp`를 만든다. 읽기에 실패하면 가장 최근 세션의 끝 값을 쓰고 `source = MANUAL`로 둔다.
2. 감지로 만든 기록은 `DRAFT`로 저장한다. 사용자가 확인하면 `CONFIRMED`, 무시하면 `DISMISSED`. 통계와 이력서는 `CONFIRMED`만 쓴다.
3. 같은 사건의 중복 초안을 막는다. 같은 종류의 `DRAFT`가 `[기본값]` 30분 안에 이미 있으면 새로 만들지 않고 갱신한다.

**L1-2. 정비 기록이 저장되면** → M3 L3-2를 부른다. 기록을 삭제하거나 항목을 바꾸면 L3-3으로 되돌린다.

**L1-3. 사고 기록**: 에어백 전개 신호가 `false → true`가 되는 순간 `ACCIDENT` 기록을 `CONFIRMED`로 바로 저장한다(사람이 확인할 여유가 없는 사건이라서). 직전 주행 세션을 연결한다.

**L1-4. 대여 기간**: `RentalPeriod(start, end, startStamp, endStamp)`. 기간이 열려 있는 동안 생긴 기록에 `rentalPeriodId`를 붙인다. 대여 모드 화면은 이 기간의 기록과 주행거리만 보여 준다. 정산 = 기간 주행거리 + 기간 에너지 기록 합계.

### M2. 주행

**L2-1. 세션 경계**

시동 이벤트 API가 없어서 신호를 조합한다(실측: 기어·총 주행거리·부팅 신호는 받을 수 있다).

| 사건 | 판단 |
|---|---|
| 세션 시작 | 기어가 P에서 D·R로 바뀜, 또는 부팅 후 총 주행거리가 늘어남 |
| 세션 끝 | 기어 P가 `[기본값]` 3분 이상 유지, 또는 앱 종료·전원 꺼짐 직전 스냅샷 |
| 충전·주유 | 세션을 끝내고 M1 감지(L2-4)로 넘긴다. 충전 중 에너지 증가가 전비 계산에 섞이지 않게 하기 위해서다 |

**L2-2. 꺼져 있던 기간 복원(GAP 세션)**

새 세션을 시작할 때 `현재 총 주행거리 − 직전 세션 끝 총 주행거리 > [기본값] 0.5km`이면 그 차이로 `GAP` 세션을 만든다. 거리와 에너지 차이만 있고 샘플은 없다. 관리항목 진행률은 총 주행거리로 계산하므로 GAP과 상관없이 늘 맞다.

**L2-3. 공회전 판정과 효율**

샘플마다 공회전 여부를 판단한다.

```
공회전 = 차속 == 0 && (기어 ∈ {P, N} || 브레이크 밟음) && 세션 진행 중
```

| 값 | EV | 연료차 |
|---|---|---|
| 전체 효율 | 세션 거리 ÷ 세션 에너지 감소(kWh) | 차량 누적 연비, 없으면 거리 ÷ 주유량 |
| 공회전 사용량 | 공회전 샘플 구간의 에너지 감소 합 | 공회전 시간 × `[기본값]` 0.8L/h(배기량별 조정 `[결정 필요]`) |
| 주행 효율 | 거리 ÷ (에너지 감소 − 공회전 사용량) | 거리 ÷ (주유량 − 공회전 추정량) |

- 에뮬레이터 차량은 전비 값을 주지 않아서(실측) EV는 앱이 직접 계산한다.
- 세션 거리가 `[기본값]` 1km 미만이면 효율을 계산하지 않는다. 짧은 주행은 오차가 너무 크다.

**L2-4. 주유·충전 감지 → M1 초안**

| | EV 충전 | 연료차 주유 |
|---|---|---|
| 시작 | 충전구 열림, 또는 정차 중 배터리 에너지 증가 | 정차 중 연료 % 증가, 또는 주유 후 거리 초기화 |
| 끝 | 충전구 닫힘 또는 기어 D | 기어 D |
| 확정 조건 | 증가량 ≥ `[기본값]` 1kWh | 증가량 ≥ `[기본값]` 5%p |
| 양 | 에너지 증가(kWh) | 연료 % 증가 × 탱크 용량 |
| 금액 초안 | 양 × 충전소 단가(모르면 사용자 기본 단가) | 양 × 오피넷 근처 주유소 단가 |
| 장소 | 현재 위치로 가장 가까운 충전소·주유소 | 같음 |

끝나면 `ENERGY` 기록을 `DRAFT`로 만들고 알림을 띄운다. 사용자는 금액만 확인한다.

**L2-5. 운행 기록** (P1): 내비 경로 시작 이벤트에서 목적지·예상 통행료를 받아 진행 중인 세션에 붙이고, 도착 이벤트에서 세션의 목적지를 확정한다(실측: 두 이벤트 모두 받음).

**L2-6. 원인 분석** (P1): 세션 샘플을 조건별로 나눠 효율을 비교한다. 에어컨 켬/끔, 정체 구간 안/밖(내비 정체 구간), 급가속 횟수(`[기본값]` 차속 증가 ≥ 10km/h/s). 차이가 `[기본값]` 5% 이상인 원인만 문장으로 보여 준다.

**L2-7. 주행 다시보기** (P1): `DriveSample`의 위치를 지도 선으로 그리고, 구간 색은 효율 또는 속도로 칠한다. 재생은 샘플 시각을 배속으로 따라간다. 위치는 앱이 켜져 있던 `MEASURED` 세션에만 있다.

### M3. 관리항목

**L3-1. 진행률**

```
거리 진행률 = (현재 총 주행거리 − 마지막 교체 총 주행거리) ÷ 주기 km
기간 진행률 = (오늘 − 마지막 교체일)을 달력 월로 센 값 ÷ 주기 개월
진행률     = 둘 중 큰 값 (한쪽 주기만 있으면 그 값)
```

| 진행률 | 상태 |
|---|---|
| < 0.8 | 양호 |
| 0.8 ~ 1.0 | 곧 교체 |
| ≥ 1.0 | 교체 필요 |

- 기간 계산은 **한 함수에서만** 달력 월로 한다. 화면과 알림이 서로 다르게 계산해 며칠씩 어긋나는 흔한 실수를 막는다.
- 현재 총 주행거리는 **항상 차량 값**을 쓴다. 마지막 기록 값을 쓰면 늦어진다.
- 마지막 교체 정보가 없는 항목은 진행률을 계산하지 않고 "마지막 교체 시점을 알려 주세요"를 보여 준다. 차량의 첫 등록 시점을 기준으로 추정할지는 `[결정 필요]`다.

**L3-2. 정비 기록 반영**: 정비 기록의 항목마다 `lastServiceOdometerKm = stamp.odometerKm`, `lastServiceAt = occurredAt`, `lastRecordId = 기록 id`로 갱신한다. 단, 이미 더 최근 교체가 있으면 덮지 않는다. 과거 기록을 나중에 입력하는 경우다.

**L3-3. 되돌리기**: 정비 기록을 삭제하면 그 항목의 정비 기록 중 다음으로 최근 것으로 상태를 다시 계산한다. 상태는 기록에서 언제든 다시 만들 수 있는 **파생값**으로 취급한다.

**L3-4. 알림**: 상태가 "곧 교체"나 "교체 필요"로 바뀌는 순간 한 번 알린다. 같은 상태에서는 반복하지 않는다.

### M4. 경유 (P1)

**L4-1. 경유 제안 시점 (충전·주유)**
- `남은 주행가능거리 < max([기본값] 50km, 남은 경로 거리 × 1.2)`일 때, 그리고 진행 중인 경로가 있을 때 한 번 제안한다.
- 경로가 없으면 정차했을 때만 근처 목록을 보여 준다.

**L4-2. 후보 고르기**
1. 현재 위치에서 진행 방향 반경 `[기본값]` 10km 안의 주유소·충전소를 가져온다(백엔드: 오피넷·충전소 API 캐시).
2. 후보마다 `추가 시간 = (현재 → 후보 → 목적지 소요시간) − (현재 → 목적지 소요시간)`을 길찾기 API로 계산한다. 반대 차선·유턴 후보는 이 값이 커져서 자연히 뒤로 밀린다.
3. `추가 시간` 오름차순, 같으면 가격 순으로 상위 3개를 남긴다. 주행 중 카드에는 1개만 보여 준다.
4. 고르면 내비 경유지로 넣는다(실측: 확인창 없이 반영). 내비가 다시 계산한 시간으로 실제 추가 시간을 보여 준다.

**L4-3. 정비소 경유 제안**: 관리항목 중 "교체 필요"가 있고 진행 중인 경로가 있으면, 목적지 근처 반경 `[기본값]` 3km의 정비소를 L4-2 방식으로 고른다. 정차 중에만 제안한다.

**L4-4. 정비소 방문 기록**
1. 목적지·경유지가 정비소였다면 도착 이벤트에서 `PendingVisit(shopName, arrivedAt, arrivedStamp)`를 저장한다.
2. 다음 세션이 시작되고(L2-1) `도착 이후 [기본값] 20분 이상` 지났으면, 그 시점에 정비 질문 카드를 띄운다. 프리셋은 "교체 필요"·"곧 교체" 항목을 먼저 보여 준다.
3. 답하면 `MAINTENANCE` 기록(`origin = NAV_VISIT`)을 만든다. 24시간 안에 답하지 않으면 버린다.

### M5. 공유 (P1 이후)

- **관리 이력서**: `CONFIRMED` 기록과 관리항목 상태를 차 한 대 단위로 묶는다. 위치·운행 경로는 넣지 않는다. 기록마다 `VehicleStamp`의 주행거리와 "차량 확인/직접 입력/수정됨" 표시를 붙인다.
- **같은 차종 순위**: 서버로는 동의한 사용자의 `(차종, 월, 주행 효율, 주행거리)`만 보낸다. 월 주행거리 `[기본값]` 300km 이상인 사람만 순위에 넣는다. 공회전을 뺀 **주행 효율**로 비교한다.
- 커뮤니티 프로필, 워키토키는 이 문서 범위 밖이다.

## 4. 지금 뼈대와의 차이

현재 `core/domain`은 연료차 기준으로만 되어 있다. P0를 시작하기 전에 고칠 것:

| 위치 | 지금 | 바꿀 것 |
|---|---|---|
| `VehicleSnapshot` | 연료 %, 연비, 총 주행거리, 경고 | 배터리 에너지·잔량·용량, 충전구, 기어, 브레이크, 에어컨, 남은 주행가능거리 추가. 연료 항목은 그대로 |
| `RefuelDraft` | 연료 전용 초안 | `Record(status = DRAFT, detail = Energy)`로 대체 |
| `VehicleFeature` | 연료·연비 위주 | EV 배터리, 충전구, 기어, 브레이크, HVAC 추가 |
| `AppDatabase` | 엔티티 없음 | `Vehicle`, `Record`, `DriveSession`, `DriveSample`, `CareItemState`, `RentalPeriod`, `PendingVisit` |
| `DemoScenario` | — | 충전 1회, 정비소 방문 1회, 공회전 포함 출퇴근 주행 시나리오 추가 |

## 5. 구현 순서 (P0)

순수 로직은 `core/domain`에서 테스트를 먼저 쓰고 만든다. 차량 없이 검증할 수 있다.

| 순서 | 할 일 | 검증 |
|---|---|---|
| 1 | 도메인 모델(§2)과 `VehicleSnapshot` 확장 | 컴파일 |
| 2 | L3-1 진행률 계산 | 달력 월 경계(1/31 → 2/28), 주기 한쪽만 있음, 마지막 교체 없음 |
| 3 | L2-1·L2-2 세션 경계와 GAP 복원 | 신호 시퀀스를 넣고 세션 개수·거리 확인 |
| 4 | L2-3 공회전·효율 | 정체 많은 세션과 없는 세션의 주행 효율 차이 |
| 5 | L2-4 충전·주유 감지 | 충전구 열림 없이 배터리 증가, 1kWh 미만 증가, 충전 중 기어 D |
| 6 | L1-1·L3-2·L3-3 기록 저장과 관리항목 반영·되돌리기 | 과거 정비 기록 늦게 입력, 기록 삭제 후 상태 |
| 7 | Room 엔티티·DAO, 저장소 구현 | 마이그레이션 없이 v1 |
| 8 | 에뮬레이터 연결: 실제 게이트웨이로 1~6 확인 | Virtual Console로 주행거리·배터리·기어 변경 |

## 6. 정할 것

1. 하이브리드를 FUEL로 둘지, 세 번째 에너지 종류로 둘지
2. 마지막 교체 정보가 없는 관리항목을 어떻게 시작할지(사용자 입력만 / 첫 등록 시점 기준 추정)
3. 연료차 공회전 연료 사용량 기준값의 출처
4. 관리 이력서 서명 방식(기기 키 / 서버 원본) — M5를 시작할 때
