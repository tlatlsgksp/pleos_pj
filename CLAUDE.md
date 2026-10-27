# pleos_pj

## 시각 자료 출력 규칙

- 차트, 다이어그램, 목업, 대시보드, 비교표, 보고서 등 시각 자료는 Claude Artifact를 사용하지 않는다. `.claude/settings.json`에서 Artifact 도구를 꺼 두었다.
- 대신 독립 실행형 `.html` 파일(CSS/JS 인라인, 외부 CDN 의존 최소화)을 `docs/visual/` 아래에 저장하고(폴더가 없으면 생성), 사용자에게 그 파일의 절대 경로를 알려준다.
