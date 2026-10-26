# 차량 스킨 시제품 (보류)

내 차를 닮은 3D 차량에 색·스티커·전개도 랩을 입히고, 스킨마다 정한 실내 무드등 색을 실제 차에 적용하는 기능의 시제품이다. 2026-10-26 본 기능에서 빼고 확대 계획으로 보류했다([기획서 6.3](../../docs/PROPOSAL_DRAFT.md)). 다시 꺼낼 때 이 문서부터 읽는다.

## 보류한 이유

- 기술은 된다. 막힌 것은 **실차 특징을 살린 3D 모델**이다.
  - Pleos 주행 화면의 3D 차량(DrivingView)은 시스템 앱 전용이라 3rd-party가 바꿀 수 없다([조사 노트 §11.2](../../docs/PLEOS_PLATFORM_RESEARCH.md)).
  - 탑재 차종(더 뉴 그랜저, 아이오닉 3)의 무료 모델이 없다. 유료 그랜저 GN7 모델은 "보도용으로만 사용(Editorial Use Only)" 제한이 붙어 앱에 쓰기 어렵다.
  - 직접 만든 미니카는 닮음이 부족했다.
- 차계부 핵심(입력 없는 기록)과 거리가 멀어 공모전 일정 안에서 우선순위가 낮다.

## 다시 꺼낼 조건

1. 주최 측이 탑재 차종의 공식 3D 에셋을 준다(설명회 질문, [API 개방 제안 §3](../../docs/PLEOS_API_PROPOSAL.md) 8번).
2. Pleos가 주행 화면 차량 테마를 연다([API 개방 제안 §2.1](../../docs/PLEOS_API_PROPOSAL.md)).
3. 외주로 모델을 만든다. 의뢰 조건은 아래 "모델 요구 조건"을 그대로 쓴다.

## 들어 있는 것

| 경로 | 내용 | 여는 법 |
|---|---|---|
| `minicar-grandeur.html` | 코드로 만든 장난감 그랜저. 외부 모델 파일이 없다. 도색, 스티커, 전개도 랩, 무드등 명령 미리보기 | 브라우저로 바로 연다(인터넷 필요, three.js CDN) |
| `ioniq5-lowpoly/` | 실제 3D 모델(아이오닉 5 로우폴리)을 쓴 같은 기능 | 폴더에서 `python3 -m http.server` 실행 후 `http://localhost:8000/` (파일을 `fetch`해서 서버가 필요하다) |
| `ioniq5-lowpoly/ioniq5.glb` | 모델 원본. CC BY 4.0, andikapratamaw ([Sketchfab](https://sketchfab.com/3d-models/hyundai-ioniq-5-lowpoly-675e78311e8440d88714bd212cb7a8fb)). 쓰는 곳에 작가와 라이선스를 표기한다 | |
| `ioniq5-lowpoly/template.png`, `wrap-fleet.png`, `bodyuv.txt` | 아래 생성기로 만든 전개도, 법인 예시 랩, 차체 랩 UV | |
| `tools/unwrap_glb.py` | GLB 차체로 전개도·랩 UV·예시 랩을 만드는 스크립트 | `python3 tools/unwrap_glb.py <모델.glb> <출력 폴더>` (Pillow 필요). 차체 재질 이름 `M_Gravity_Gold_Matte`와 축(+Y 위, +Z 앞, +X 왼쪽)은 모델에 맞게 고친다 |

claude.ai 비공개 미리보기(작성자 계정에서만 열린다): [아이오닉 스튜디오](https://claude.ai/artifact/UUQejyoGknjxZBJdU9EZx7), [미니카 그랜저](https://claude.ai/artifact/5doJuFxVBvwYQ1wGXEcvpt)

## 동작 원리

- **도색**: 차체 재질만 찾아 색과 질감(유광·무광)을 바꾼다.
- **스티커**: 차를 누른 점에서 표면 법선 방향으로 그림을 투영한다(three.js `DecalGeometry`). 끌면 표면을 따라 옮겨진다. 스티커 하나는 그림 ID, 표면 좌표, 법선, 크기(cm), 회전 다섯 값이라 스킨 데이터가 수백 바이트다. 3D 모델은 주고받지 않는다.
- **전개도 랩**: 차체 삼각형을 바라보는 방향(왼쪽·오른쪽·위·앞·뒤)으로 나눠 바깥에서 본 모습 그대로 펴고, 같은 축척으로 한 장에 배치한다. 실제 랩 시공 도면과 같은 방식이다. 이 좌표를 차체 UV로 쓰면 도면에 그린 디자인이 차에 그대로 입혀진다. 밑면은 인쇄하지 않는다.
- **무드등**: `Light.setAmbientLightSwitch(cabin, ON)`, `setAmbientLightBrightnessColor(cabin, 0xRRGGBB)`. `cabin`은 `SeatArea(ROW_1_LEFT, ROW_1_RIGHT, ROW_2_LEFT, ROW_2_CENTER, ROW_2_RIGHT)`(영역 `0x75`). 권한은 `pleos.car.permission.CONTROL_CAR_LIGHTS`(normal). 에뮬레이터에서 차량 HAL 로그 `set ambient lights color`까지 확인했다. 다시 읽으면 값이 그대로라 실제 반영은 미확인이다.

## 모델 요구 조건 (구매·외주 기준)

1. 차체 도장이 **별도 재질**로 분리되어 있을 것(색·랩).
2. 유리·램프·그릴이 차체와 **다른 재질**일 것. 불투명 부품과 유리를 한 반투명 재질로 묶으면 실시간 엔진에서 뒤쪽 부품이 비친다(아이오닉 모델에서 겪었다. `index.html`의 `M_Ioniq` 처리 참고).
3. 차체 면이 매끈하고 면 수가 차량 앱에 맞을 것(수만 개 이하, RAM 400MB 기준).
4. 상업 사용이 가능한 라이선스. 브랜드 차량은 "보도용" 제한이 흔하다.
5. 가능하면 랩용 UV 전개도 포함. 없으면 `tools/unwrap_glb.py`로 만든다.

## 모델 조사 결과 (2026-10-26)

- **Sketchfab 무료**: 아이오닉 5 로우폴리(CC BY, 면 1.2만)가 가장 좋았다. 아반떼(CN7)는 [Ultra Low Poly](https://sketchfab.com/3d-models/none-b30d0baf6c3d4ca4adf2f484a90fc861)(CC BY, 면 2만, 재질 14개)가 쓸 만하다. 투싼은 [niev 2022](https://sketchfab.com/3d-models/none-574deec95f094e9192b0023d3d4ad607)(CC BY, 면 50만, 출처 설명 없음)가 조건부, 최고 품질인 [ddiaz-design 2025](https://sketchfab.com/3d-models/none-9f633163211e418f93aedaf6da97435d)는 비상업 전용이라 원작자 허락이 필요하다. PV5는 쓸 만한 무료 모델이 없다.
- **걸러야 하는 모델**: 비상업 원본과 면 수가 똑같은 CC BY 재업로드(예: `tangwang920` 업로드), 설명에 게임에서 추출했다고 적힌 모델(예: GTA5 모드 추출 아반떼), 재질이 1개뿐인 대량 업로드(AI 생성 추정).
- **Kenney Car Kit**(CC0, 45종): 장난감 스타일로 귀엽지만 일반 차종이라 실차 특징이 없고, 색이 팔레트 텍스처 한 장이라 도색이 번거롭다.
- **AI 3D 생성**(Rodin, Tripo, Meshy, Hunyuan3D, TRELLIS): 결과가 한 덩어리 메시라 도색·랩이 안 된다. 보기용 모델로만 쓸 만하다.
- **가격**: 마켓 구매 로우폴리 $50~150, 실사 $100~400(브랜드 차량은 보도용 제한 주의). 외주는 로우폴리 $50~200, 게임용 완성품 수백 달러~€1,000 수준.

## 만들며 겪은 함정

- 전개도에서 옆면 좌우를 바깥에서 본 방향과 반대로 펴면 랩 글자가 거울처럼 찍힌다. 축을 바꿀 때마다 옆면 글자 방향을 렌더로 확인한다.
- 캔버스에서 `destination-in`으로 잘라낼 때 삼각형마다 따로 칠하면 마지막 삼각형만 남는다. 마스크를 한 장 그린 뒤 한 번에 잘라낸다.
- 3D 화면 칸이 옆 패널 높이를 따라 늘어나면 차가 확대돼 보인다. 화면 칸 높이를 창 높이에 고정한다.
- `ExtrudeGeometry`의 모서리 둥글림(bevel)은 윤곽을 바깥으로 키워 바퀴 아치를 그만큼 좁힌다.
- three.js r147에서 색 상수는 sRGB로 넣으면 출력 변환을 한 번 더 거쳐 빨강이 분홍으로 보인다. `convertSRGBToLinear()`를 쓴다.
- 헤드리스 Chrome으로 확인할 때는 `--use-angle=swiftshader --enable-unsafe-swiftshader --disable-accelerated-2d-canvas`를 줘야 캔버스 텍스처가 보인다.
