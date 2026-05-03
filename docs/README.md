# Backend Docs 관리 기준

`docs/`는 프론트와 팀원이 보는 백엔드 연동 문서의 기준 위치다.

## 문서 원칙

- `docs/` 안의 문서는 현재 `main` 브랜치 백엔드 구현과 충돌하지 않아야 한다.
- API 동작이 바뀌면 관련 문서를 함께 수정한다.
- 날짜별 handoff 문서는 당시 작업 요약이지만, 명백히 틀린 API 상태나 에러 메시지는 최신 구현 기준으로 보정한다.
- 개인 메모나 실험 노트는 `docs/`에 두지 않는다.
- Swagger 기준은 개발 서버의 실제 구현 스펙인 `/v3/api-docs.yaml`을 우선한다.

## 문서 목록

| 문서 | 용도 |
| --- | --- |
| `FRONTEND_HANDOFF_20260502.md` | 2026-05-02 기준 프론트 전달 요약 |
| `FRONTEND_DEVELOPMENT_SUMMARY_20260501.md` | 온보딩, 루틴, 운동, 컨디션, 퀘스트, AI 연동 흐름 종합 |
| `ONBOARDING_API_FRONTEND_GUIDE.md` | 온보딩 입력 필드와 응답 필드 |
| `ROUTINE_API_FRONTEND_GUIDE.md` | 추천 루틴, 직접 루틴 생성, 활성화, 세션 구조 |
| `EXERCISE_API_FRONTEND_GUIDE.md` | 운동 목록, 상세, 즐겨찾기 |
| `QUEST_API_FRONTEND_GUIDE.md` | 오늘 퀘스트 생성/조회/완료, AI 연동 방식 |
| `FRONTEND_SWAGGER_GUIDE.md` | Swagger 확인 방법과 응답 포맷 |
| `CICD.md` | 개발 서버 CI/CD 흐름 |
| `DEVELOPMENT.md` | 로컬 개발 실행 기준 |

## 현재 프론트 우선 확인 순서

1. `FRONTEND_HANDOFF_20260502.md`
2. `FRONTEND_DEVELOPMENT_SUMMARY_20260501.md`
3. 필요한 기능별 상세 문서
4. Swagger UI 또는 `/v3/api-docs.yaml`
