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
| `FRONTEND_HANDOFF_20260506.md` | 2026-05-06 기준 스토리 채팅 목록/입장 정보 API, 임시 비밀번호 계정 복구, 닉네임 중복 체크, 마이페이지/계정 수정 요약 |
| `FRONTEND_HANDOFF_20260505.md` | 2026-05-05 기준 세계관 이미지 절대 URL, 세계관 카드 미리보기 API, AI 스토리 프록시 최신화, 개발 서버 테스트 데이터 요약 |
| `FRONTEND_HANDOFF_20260504.md` | 2026-05-04 기준 홈 상단 세계관 슬라이드, 세계관 랭킹, 이번 주 활동 랭킹, 레벨/EXP/Gold 보상 정책, 즐겨찾기 조회, 오늘의 루틴 조회, 주간 통계 조회, 루틴 수정/삭제, 루틴 단위 퀘스트, 테스트 계정, 액세스 토큰 변경 요약 |
| `REWARD_POLICY_GUIDE.md` | EXP, 레벨, Gold 지급 기준과 중복 지급 방지 구조 |
| `FRONTEND_HANDOFF_20260502.md` | 2026-05-02 기준 프론트 전달 요약 |
| `FRONTEND_DEVELOPMENT_SUMMARY_20260501.md` | 온보딩, 루틴, 운동, 컨디션, 퀘스트, AI 연동 흐름 종합 |
| `ONBOARDING_API_FRONTEND_GUIDE.md` | 온보딩 입력 필드와 응답 필드 |
| `ROUTINE_API_FRONTEND_GUIDE.md` | 추천 루틴, 직접 루틴 생성/수정/삭제, 활성화, 세션 구조 |
| `EXERCISE_API_FRONTEND_GUIDE.md` | 운동 목록, 상세, 즐겨찾기 |
| `QUEST_API_FRONTEND_GUIDE.md` | 오늘 퀘스트 생성/조회/완료, AI 연동 방식 |
| `WORLD_RANKING_API_FRONTEND_GUIDE.md` | 세계관 랭킹 요약/전체보기, 장르 필터, 무한스크롤 |
| `AI_QUEST_API_GUIDE.md` | AI 서버가 userId로 오늘 퀘스트를 조회하는 백엔드 연동 방식 |
| `FRONTEND_SWAGGER_GUIDE.md` | Swagger 확인 방법과 응답 포맷 |
| `CICD.md` | 개발 서버 CI/CD 흐름 |
| `DEVELOPMENT.md` | 로컬 개발 실행 기준 |

## 현재 프론트 우선 확인 순서

1. `FRONTEND_HANDOFF_20260506.md`
2. `FRONTEND_HANDOFF_20260505.md`
3. `FRONTEND_HANDOFF_20260504.md`
4. `FRONTEND_HANDOFF_20260502.md`
5. `FRONTEND_DEVELOPMENT_SUMMARY_20260501.md`
6. 필요한 기능별 상세 문서
7. Swagger UI 또는 `/v3/api-docs.yaml`
