# pleos_pj

## 시각 자료 출력 규칙

- 차트, 다이어그램, 목업, 대시보드, 비교표, 보고서 등 시각 자료는 Claude Artifact를 사용하지 않는다. `.claude/settings.json`에서 Artifact 도구를 꺼 두었다.
- 대신 독립 실행형 `.html` 파일(CSS/JS 인라인, 외부 CDN 의존 최소화)을 `docs/visual/` 아래에 저장하고(폴더가 없으면 생성), 사용자에게 그 파일의 절대 경로를 알려준다.

## Impeccable 자동 init

- 저장소 루트에 `PRODUCT.md`가 없으면, 앱 기획·화면·디자인·UI 코드 작업을 시작하기 전에 사용자가 시키지 않아도 Impeccable 스킬의 `init`(`/impeccable init`)을 먼저 실행한다. 다른 작업(깃, 빌드, 조사)만 할 때는 실행하지 않는다.
- init은 저장소의 기획 문서(`docs/`)와 코드를 먼저 읽고, 모르는 것만 AskUserQuestion으로 묻는다. 끝나면 원래 하던 작업으로 돌아간다.
- 플랫폼은 `android`다(Pleos Connect, AAOS 차량 앱). 화면은 2560×1440, 160dpi 차량 디스플레이 기준이다.
- `PRODUCT.md`와 `.impeccable/config.json`은 커밋 대상이다.
