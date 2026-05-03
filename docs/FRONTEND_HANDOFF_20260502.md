# 2026-05-02 프론트 전달 사항

## 배포 상태

- 백엔드 `main` 배포 완료.
- 개발 서버 컨테이너: `capstone-backend`
- 헬스체크:
  - `GET /actuator/health` 정상
  - `GET /api/health` 정상
- 응답 시간/날짜는 한국시간 기준으로 내려가도록 테스트 환경까지 고정함.

## 로그인 후 기본 분기

로그인 후에는 먼저 사용자 상태를 조회한다.

```http
GET /api/users/me
Authorization: Bearer {accessToken}
```

주요 필드:

```json
{
  "onboardingCompleted": true,
  "requiresOnboarding": false,
  "todayConditionCompleted": true,
  "activeRoutineId": 1,
  "routineSetupRequired": false
}
```

- `onboardingCompleted=false`면 온보딩 입력 화면으로 이동.
- `routineSetupRequired=true`면 루틴 설정 분기 화면으로 이동.
- `todayConditionCompleted=false`는 채팅방/스토리/퀘스트 진입 시점에 컨디션 입력을 유도하면 됨.

## 온보딩 완료 후 루틴 설정 분기

온보딩 저장:

```http
PUT /api/users/me/onboarding-profile
Authorization: Bearer {accessToken}
Content-Type: application/json
```

온보딩 완료 직후 바로 추천 루틴을 강제 호출하지 말고 아래 분기 화면을 보여준다.

```text
1. 추천 루틴으로 시작하기
2. 제가 알아서 만들래요
3. 나중에 할게요
```

### 1. 추천 루틴으로 시작하기

```http
GET /api/routines/recommendations
Authorization: Bearer {accessToken}
```

사용자가 추천 루틴을 선택하면:

```http
POST /api/routines/{routineId}/activate
Authorization: Bearer {accessToken}
```

선택한 루틴은 사용자 루틴으로 복사되고 활성화된다.

### 2. 제가 알아서 만들래요

운동 목록 화면으로 이동한다.

```http
GET /api/exercises
Authorization: Bearer {accessToken}
```

운동 상세 카드 클릭 시:

```http
GET /api/exercises/{exerciseId}
Authorization: Bearer {accessToken}
```

운동 좋아요:

```http
PUT /api/users/me/exercises/{exerciseId}/favorite
Authorization: Bearer {accessToken}
```

직접 루틴 생성:

```http
POST /api/routines/custom
Authorization: Bearer {accessToken}
Content-Type: application/json
```

예시:

```json
{
  "name": "내 전신 루틴",
  "description": "직접 만든 전신 루틴",
  "activate": true,
  "sessions": [
    {
      "dayOfWeek": "MONDAY",
      "sessionName": "월요일 전신",
      "sessionType": "full_body",
      "estimatedMinutes": 40,
      "items": [
        {
          "exerciseId": 1,
          "sets": 3,
          "reps": 12,
          "restSec": 60
        }
      ]
    }
  ]
}
```

- `activate=true`면 생성 직후 활성 루틴으로 설정됨.
- `sessions`는 요일별 운동 세션.
- 각 `items`는 해당 세션에 들어가는 운동.
- `sets`, `reps`, `durationSec` 중 최소 하나는 있어야 함.

내 루틴 목록:

```http
GET /api/users/me/routines
Authorization: Bearer {accessToken}
```

활성 루틴 조회:

```http
GET /api/users/me/routines/active
Authorization: Bearer {accessToken}
```

루틴 상세:

```http
GET /api/routines/{routineId}
Authorization: Bearer {accessToken}
```

상세 응답에는 요일별 `sessions`가 포함된다.

```json
{
  "id": 1,
  "name": "주 3회 입문 루틴",
  "sessions": [
    {
      "dayOfWeek": "MONDAY",
      "sessionName": "월요일 전신",
      "sessionType": "full_body",
      "estimatedMinutes": 40,
      "items": []
    }
  ]
}
```

### 3. 나중에 할게요

홈 진입은 가능하게 둔다.

단, 활성 루틴이 없으면 오늘 퀘스트 생성 시 아래 에러가 날 수 있다.

```json
{
  "code": "ACTIVE_ROUTINE_REQUIRED",
  "detail": "오늘 퀘스트를 생성하려면 추천 루틴을 선택하거나 내 루틴을 먼저 만들어 주세요."
}
```

## 컨디션 입력과 퀘스트

컨디션 입력:

```http
PUT /api/conditions/today
Authorization: Bearer {accessToken}
Content-Type: application/json
```

사진 UI 기준 요청 필드:

```json
{
  "conditionLevel": 3,
  "sleepScore": 3,
  "stressScore": 2,
  "energyLevel": 3
}
```

- `memo`는 사용하지 않음.
- 응답에는 `conditionScore`, `exerciseMultiplier`가 포함됨.

오늘 퀘스트 조회:

```http
GET /api/quests/today
Authorization: Bearer {accessToken}
```

- 퀘스트는 컨디션 입력 시점이 아니라 `GET /api/quests/today` 호출 시 lazy 생성됨.
- 같은 날 다시 호출하면 기존 퀘스트를 재사용함.
- 루틴이 있는 날이면 루틴 세션 일부 운동 기반 퀘스트.
- 루틴이 없는 날이면 휴식일 퀘스트.
- 컨디션이 낮으면 회복 퀘스트 또는 목표량 축소.

## AI 스토리 API 호출 방식

프론트는 AI 스토리 플레이 요청에 `user_id`를 보내지 않는다.

백엔드가 JWT에서 로그인 사용자 ID를 꺼내 AI 서버 요청의 `user_id`로 주입한다. 프론트가 실수로 `user_id`를 보내도 백엔드 값으로 덮어쓴다.

```http
POST /api/stories/play
Authorization: Bearer {accessToken}
Content-Type: application/json
```

요청:

```json
{
  "scenario_id": 1,
  "user_message": "주변을 둘러본다.",
  "choice_id": null,
  "restart": false
}
```

처음 세계관 입장 또는 처음부터 시작:

```json
{
  "scenario_id": 1,
  "restart": true
}
```

## 세계관/스토리 DB 반영

로컬 DB와 개발 서버 DB에 스토리 데이터 safe merge 완료.

기존 데이터는 삭제하거나 덮어쓰지 않고, 없는 컬럼/테이블/레코드만 추가했다.

추가된 스키마:

```text
scenarios.title
scenarios.summary
scenarios.thumbnail_url
scenarios.genre
scenarios.is_active
story_scene_states
```

현재 스토리 관련 데이터 수:

```text
scenarios: 3
scenario_chapters: 17
chapter_choices: 51
chapter_details: 51
character_profiles: 14
story_progress: 7
story_play_logs: 83
story_scene_states: 8
```

주의:

- 세계관 목록/랭킹 조회용 백엔드 API는 아직 별도 구현 전.
- DB에는 표시용 컬럼과 데이터가 준비됨.
- 다음 백엔드 작업은 `GET /api/worlds`, `GET /api/worlds/rankings` 같은 세계관 API 구현이 적절함.

## Swagger

Swagger는 배포된 백엔드 기준으로 확인하면 된다.

```text
http://100.89.171.113:8080/swagger-ui/index.html
```

프론트에서 API가 안 보이면 먼저 개발 서버 컨테이너가 최신 배포인지 확인해야 한다.
