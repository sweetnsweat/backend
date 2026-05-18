# Codespaces Backend Handoff - 2026-05-19

이 문서는 노트북 없이 GitHub Codespaces에서 백엔드 작업을 이어갈 때 AI가 먼저 읽을 맥락이다.

## 프로젝트 한 줄 요약

Sweet & Sweat는 운동 루틴, 컨디션, 일일 퀘스트, 보상/상점, AI 세계관 채팅, 푸시 알림, 랭킹/배틀형 경쟁 화면을 연결하는 캡스톤 피트니스 앱이다.

백엔드는 Spring Boot API 서버이며 모바일 앱과 AI 서버 사이의 인증/데이터/프록시 중심 역할을 맡는다.

## 레포와 기준 브랜치

- 레포: `sweetnsweat/backend`
- 기준 브랜치: `main`
- 개발 서버: `http://100.89.171.113:8080`
- Swagger: `http://100.89.171.113:8080/swagger-ui/index.html`
- 공통 성공 응답: `ApiResponse<T>`
- 공통 에러 응답: Spring `ProblemDetail` 기반 에러 응답

Codespaces에서 시작하면 먼저 `main` 최신화를 확인한다.

```bash
git status --short --branch
git pull --ff-only origin main
```

## 실행 환경 핵심

로컬/개발 서버 설정은 `src/main/resources/application.properties`, `.env.example`, `docs/DEVELOPMENT.md`, `docs/CICD.md`를 우선 확인한다.

주요 환경 변수:

| 이름 | 용도 |
| --- | --- |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | PostgreSQL 연결 |
| `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD` | Redis 연결 |
| `JWT_SECRET` | JWT 서명 키. 32자 이상 필요 |
| `AI_BASE_URL` | AI 서버 프록시 대상 |
| `MEDIA_BASE_URL` | AI/미디어 이미지 URL 기준 |
| `FIREBASE_ENABLED` | FCM 실제 발송 활성화 |
| `FIREBASE_PROJECT_ID` | Firebase 프로젝트 ID |
| `FIREBASE_SERVICE_ACCOUNT_PATH` | Firebase service account JSON 경로 |

Jenkins 배포는 `Jenkinsfile` 기준으로 `main` push 시 개발 서버 컨테이너를 재빌드한다. FCM 배포 쪽은 service account 파일을 컨테이너에 read-only mount하는 흐름이다.

## Codespaces secrets 등록 상태

2026-05-19 기준 GitHub repo `sweetnsweat/backend`의 Codespaces secrets에 아래 이름들이 등록되어 있다. 값은 GitHub secret으로만 저장되어 있고 레포에는 커밋하지 않는다.

```text
AI_BASE_URL
API_REQUEST_LOGGING_ENABLED
DB_PASSWORD
DB_URL
DB_USERNAME
DEV_LOGS_ENABLED
FIREBASE_ENABLED
FIREBASE_PROJECT_ID
JWT_ACCESS_TOKEN_SECONDS
JWT_ISSUER
JWT_REFRESH_TOKEN_SECONDS
JWT_SECRET
MEDIA_BASE_URL
POSTGRES_PASSWORD
REDIS_HOST
REDIS_PORT
SERVER_PORT
```

Codespaces 기본값은 Codespace 안에서 로컬 PostgreSQL/Redis를 띄워 개발하는 기준이다.

- `POSTGRES_PASSWORD`와 `DB_PASSWORD`는 같은 랜덤 값으로 맞춰 두었다.
- `DB_URL`은 `jdbc:postgresql://localhost:5432/postgres` 기준이다.
- `REDIS_HOST`는 `localhost` 기준이다.
- `AI_BASE_URL`, `MEDIA_BASE_URL`은 Codespace 내부에서 AI 서버를 같이 띄우는 전제로 `localhost:8000` 기준이다.
- `FIREBASE_ENABLED=false`로 등록했다. Codespaces에는 Firebase service account JSON 파일이 없으므로 실제 FCM 발송은 개발 서버/Jenkins 설정을 사용한다.

## Codespaces Tailscale 접속

레포에는 `.devcontainer/devcontainer.json`이 추가되어 있고, 새 Codespace를 만들면 Tailscale feature가 함께 설치된다.

설정 출처:

- Tailscale Codespaces feature: `ghcr.io/tailscale/codespace/tailscale`
- Tailscale feature가 요구하는 `/dev/net/tun` 접근을 위해 `runArgs`에 `--device=/dev/net/tun`을 둔다.
- Docker Compose 사용을 위해 `docker-outside-of-docker` feature도 같이 둔다.

자동 로그인하려면 GitHub Codespaces secret에 아래 이름을 추가해야 한다.

```text
TS_AUTH_KEY
```

이 값은 Tailscale admin console에서 만든 auth key다. 현재 2026-05-19 기준으로 `TS_AUTH_KEY` 값은 아직 등록하지 않았다. 이 secret이 등록되면 새 Codespace 시작 시 feature가 자동으로 다음과 같은 동작을 수행한다.

```bash
tailscale up --accept-routes --authkey=$TS_AUTH_KEY
```

`TS_AUTH_KEY` 없이도 수동 로그인은 가능하다. Codespace 터미널에서 아래 명령을 실행하면 로그인 URL이 출력된다.

```bash
sudo tailscale up --accept-routes
```

Tailscale 연결 확인:

```bash
tailscale status
curl http://100.89.171.113:8080/api/health
ssh 100.89.171.113 "docker ps --format '{{.Names}} {{.Status}}'"
```

주의: 기존 Codespace에는 devcontainer 변경이 자동 적용되지 않는다. 기존 Codespace를 계속 쓸 경우 Codespaces UI에서 rebuild container를 실행한다.

Codespaces에서 backend만 빠르게 띄울 때는 Postgres/Redis를 먼저 준비하고 다음 중 하나를 사용한다.

```bash
docker compose up -d postgres redis
./gradlew bootRun
```

또는 backend까지 컨테이너로 실행한다.

```bash
docker compose up -d --build
```

## 현재 구현된 백엔드 기능

### 인증/회원

- 회원가입, 로그인, 로그아웃, refresh token 재발급
- 닉네임 중복 체크
- 아이디 찾기, 비밀번호 재설정 요청
- 비밀번호 변경
- 마이페이지/회원정보 수정
- 최근 변경: 회원정보 수정에서 생년월일 업데이트 허용

관련 문서:

- `docs/FRONTEND_HANDOFF_20260517.md`
- `docs/FRONTEND_HANDOFF_20260508.md`

### 온보딩/루틴/운동/컨디션

- 온보딩 프로필 저장
- 추천 루틴 선택/활성화
- 직접 루틴 생성/수정/삭제
- 운동 목록/상세/즐겨찾기
- 컨디션 저장 및 컨디션 점수/운동 배율 계산

관련 문서:

- `docs/ONBOARDING_API_FRONTEND_GUIDE.md`
- `docs/ROUTINE_API_FRONTEND_GUIDE.md`
- `docs/EXERCISE_API_FRONTEND_GUIDE.md`
- `docs/FRONTEND_DEVELOPMENT_SUMMARY_20260501.md`

### 퀘스트 완료

구현 상태:

- `GET /api/quests/today`
- `GET /api/quests/today/by-user?userId={userId}`
- `PATCH /api/quests/{questId}/complete`

현재 동작:

- KST 기준 오늘 퀘스트를 조회한다.
- 오늘 퀘스트가 없으면 활성 루틴, 오늘 컨디션, 오늘 요일 세션 기준으로 자동 생성한다.
- 오늘 루틴 세션이 있으면 `ROUTINE`, 없으면 `OFF_DAY`, 컨디션이 낮으면 `RECOVERY` 퀘스트가 생성된다.
- 완료 요청 body는 생략 가능하다.
- 요청 body를 보내면 `progressValue`, `proof`를 저장한다.
- 이미 완료된 퀘스트를 다시 완료해도 EXP/Gold는 중복 지급되지 않는다.
- 보상은 `RewardService`가 `user_exp_logs`, `wallets`, `wallet_transactions`를 통해 지급한다.

관련 코드:

- `src/main/java/com/capstone/backend/quest/controller/QuestController.java`
- `src/main/java/com/capstone/backend/quest/service/QuestService.java`
- `src/main/java/com/capstone/backend/reward/service/RewardService.java`
- `src/main/java/com/capstone/backend/reward/policy/QuestRewardPolicy.java`

남은 백엔드 작업 후보:

- 퀘스트 완료 proof 검증 강화
- Health Connect 기록 기반 자동 완료 또는 반자동 완료
- 퀘스트 완료 후 FCM 알림 발송
- 퀘스트 완료 이벤트를 배틀/랭킹 도메인에 명시적으로 연결

관련 문서:

- `docs/QUEST_API_FRONTEND_GUIDE.md`
- `docs/AI_QUEST_API_GUIDE.md`
- `docs/REWARD_POLICY_GUIDE.md`

### FCM 푸시 알림

구현 상태:

- `POST /api/push-tokens`
- `DELETE /api/push-tokens/{tokenId}`
- `POST /api/notifications/test`
- `user_push_tokens` 저장
- Firebase Admin SDK 기반 실제 발송 sender
- Firebase 비활성 시 noop sender
- Jenkins 개발 서버 배포 시 Firebase service account mount

현재 동작:

- 모바일 앱이 발급한 FCM registration token을 로그인 사용자에게 등록한다.
- 같은 토큰이 다시 들어오면 사용자/플랫폼/deviceId/lastSeenAt을 갱신한다.
- 로그아웃 또는 알림 해제 시 토큰을 비활성화한다.
- 테스트 발송 API는 현재 사용자의 활성 토큰으로 발송한다.

관련 코드:

- `src/main/java/com/capstone/backend/notification/controller/NotificationController.java`
- `src/main/java/com/capstone/backend/notification/service/NotificationService.java`
- `src/main/java/com/capstone/backend/notification/service/FcmPushNotificationSender.java`
- `src/main/java/com/capstone/backend/notification/config/FirebaseMessagingConfig.java`
- `src/main/java/com/capstone/backend/notification/entity/UserPushToken.java`
- `Jenkinsfile`

남은 백엔드 작업 후보:

- 퀘스트 발급/완료 알림 스케줄링
- 루틴 리마인더 알림 스케줄링
- 배틀 매칭/결과 알림
- 사용자별 알림 설정 `pushEnabled`, `pushQuestEnabled`, `pushRoutineEnabled`, `pushCompetitionEnabled`를 실제 발송 조건에 연결
- 실패 토큰 정리 정책

### AI 세계관/채팅

구현 상태:

- `POST /api/stories/generate`
- `POST /api/stories/play`
- `POST /api/stories/play/start`
- `GET /api/stories/play/history`
- `GET /api/stories/scenarios`
- `GET /api/stories/scenarios/{scenarioId}`
- `GET /api/stories/chats`
- `GET /api/stories/chats/{scenarioId}`

현재 동작:

- 프론트는 `user_id`를 보내지 않는다.
- 백엔드가 JWT 사용자 ID를 AI 서버 요청에 주입한다.
- AI proxy는 기존 Authorization 헤더를 AI 서버 호출에도 전달한다.
- `/api/stories/play/start`는 Swagger에 노출되어 있다.
- `/api/stories/chats`는 이미 입장한 세계관 채팅방 목록을 반환한다.
- `/api/stories/chats/{scenarioId}`는 채팅방 메타데이터, 전체 캐릭터 목록, 최근 대화 `recentMessages`를 함께 반환한다.
- 한 채팅방에 여러 캐릭터가 있을 수 있으므로 상세 응답의 `characters` 배열을 기준으로 처리해야 한다.

관련 코드:

- `src/main/java/com/capstone/backend/ai/controller/AiStoryProxyController.java`
- `src/main/java/com/capstone/backend/ai/service/AiStoryRequestFactory.java`
- `src/main/java/com/capstone/backend/story/controller/StoryChatController.java`
- `src/main/java/com/capstone/backend/story/service/StoryChatService.java`
- `src/main/java/com/capstone/backend/ai/controller/AiStorySwaggerExamples.java`

관련 문서:

- `docs/FRONTEND_HANDOFF_20260506.md`
- `docs/FRONTEND_HANDOFF_20260508.md`
- `docs/FRONTEND_SWAGGER_GUIDE.md`
- `docs/WORLD_RANKING_API_FRONTEND_GUIDE.md`

### 배틀/경쟁

현재 백엔드 상태:

- 별도 `battle` 도메인/API는 아직 없다.
- 현재 구현된 경쟁성 API는 `GET /api/rankings/weekly-activity`이다.
- 이 API는 KST 기준 이번 주 완료 퀘스트의 `rewardExp` 합산으로 랭킹을 계산한다.
- 모바일의 배틀 화면이 실제 API와 연결되려면 백엔드에 배틀 도메인을 새로 설계해야 한다.

관련 코드:

- `src/main/java/com/capstone/backend/ranking/controller/RankingController.java`
- `src/main/java/com/capstone/backend/ranking/service/RankingService.java`
- `src/main/java/com/capstone/backend/quest/repository/UserQuestRepository.java`

배틀 백엔드 구현 후보:

1. `battle_rooms` 또는 `competitions` 테이블 추가
2. `battle_participants` 테이블 추가
3. 기간 타입: `DAILY`, `WEEKLY`
4. 매칭 기준: 사용자 레벨/최근 활동량/랜덤 중 하나 선택
5. 점수 기준: 기간 내 완료 퀘스트 EXP 합산 또는 완료 횟수
6. API 초안:
   - `POST /api/battles/match`
   - `GET /api/battles/current`
   - `GET /api/battles/{battleId}`
   - `GET /api/battles/{battleId}/result`
7. FCM 연동:
   - 매칭 완료
   - 상대 추월
   - 배틀 종료/결과 확정

## Codespaces에서 이어갈 때 우선순위

1. 현재 하려는 작업이 문서 작업인지 API 구현인지 먼저 확정한다.
2. API 구현이면 새 브랜치를 만들고 진행한다.
3. 기존 문서와 Swagger가 다르면 실제 구현과 `/v3/api-docs.yaml`을 기준으로 맞춘다.
4. 퀘스트/FCM/배틀은 서로 연결될 가능성이 높으므로 이벤트 흐름을 먼저 정한다.

추천 작업 순서:

1. 배틀 API 최소 모델 설계
2. 퀘스트 완료 이벤트를 배틀 점수에 반영
3. FCM 알림 설정 필드와 실제 발송 조건 연결
4. 퀘스트 완료 proof 검증 또는 Health Connect 연동
5. Swagger 예시와 프론트 handoff 문서 업데이트

## 검증 명령

Codespaces에서 DB/Redis 환경이 준비된 뒤 다음을 우선 사용한다.

```bash
./gradlew test
./gradlew bootJar
```

개발 서버 또는 로컬 서버가 떠 있으면:

```bash
curl http://localhost:8080/api/health
curl http://localhost:8080/v3/api-docs.yaml
```

## 주의할 점

- `main`에 이미 병합된 FCM/Jenkins 변경을 되돌리지 않는다.
- `GET /api/quests/today/by-user`는 AI 서버 데모 편의를 위한 무토큰 API이므로 보안 범위를 재검토할 수 있다.
- 배틀은 아직 실제 백엔드 API가 없으므로 모바일 화면만 보고 구현됐다고 가정하면 안 된다.
- 퀘스트 완료는 이미 보상 중복 방지가 들어가 있으므로 중복 지급 테스트를 유지해야 한다.
- Firebase service account JSON은 커밋하지 않는다.
- Codespaces에는 로컬 Mac의 Docker, Colima, Android emulator 상태가 따라오지 않는다.
