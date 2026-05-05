# 2026-05-05 프론트 전달 사항

오늘 변경된 프론트 연동 영향은 세계관 이미지 URL, 세계관 카드 미리보기 모달 API, AI 스토리 프록시 API 최신화, 개발 서버 테스트 데이터다.

## 1. 이미지 URL 응답 방식

DB에는 `/media/assets/...` 상대 경로를 저장하지만, 백엔드 응답은 프론트가 바로 사용할 수 있는 절대 URL로 변환해서 내려준다.

개발 서버 기준:

```text
http://100.89.171.113:8000/media/assets/...
```

영향 API:

```http
GET /api/home/world-banners
GET /api/worlds/rankings
GET /api/worlds/rankings/full
GET /api/worlds/{scenarioId}/preview
```

프론트는 응답의 `imageUrl`, `backgroundImageUrl`, `thumbnailUrl`, `worldImageUrl`, `playerImageUrl`, `representativeCharacter.imageUrl`, `characters[].imageUrl`을 그대로 이미지 URI로 사용하면 된다.

## 2. 세계관 카드 미리보기 API

세계관 랭킹 카드 클릭 시 모달에 표시할 상세 정보다. 이 API는 모달 표시와 사용자 진행 상태 확인까지만 담당한다. 버튼 문구와 입장 이후 화면 전환은 프론트/스토리 담당 흐름에서 처리한다.

```http
GET /api/worlds/{scenarioId}/preview
Authorization: Bearer {accessToken}
```

응답 핵심:

```json
{
  "scenario": {
    "id": 4,
    "title": "월하검귀는 다시 웃지 않는다",
    "summary": "복수와 회귀를 다루는 무협 세계관",
    "genre": "무협 회귀 복수 로맨스",
    "genres": ["무협", "회귀", "복수", "로맨스"],
    "thumbnailUrl": "http://100.89.171.113:8000/media/assets/thumb.png",
    "worldImageUrl": "http://100.89.171.113:8000/media/assets/world.png",
    "playerImageUrl": "http://100.89.171.113:8000/media/assets/player.png",
    "playerDescription": "이 세계관에서 사용자가 맡게 되는 역할 설명",
    "active": true
  },
  "ranking": {
    "metric": "ACTIVE_CHAT_COUNT",
    "score": 2
  },
  "representativeCharacter": {
    "id": 12,
    "name": "천류하",
    "title": "검귀",
    "type": "main",
    "imageUrl": "http://100.89.171.113:8000/media/assets/character.png",
    "quote": "네 뒤를 쫓아가도...",
    "tags": ["검귀", "복수"],
    "representative": true
  },
  "characters": [],
  "entry": {
    "canEnter": true,
    "hasProgress": true,
    "progressStatus": "IN_PROGRESS"
  }
}
```

`entry.hasProgress`는 해당 로그인 사용자가 이 세계관에 진행 기록이 있는지 여부다. `entry.progressStatus`는 있으면 진행 상태 문자열이고, 없으면 `null`이다.

## 3. AI 스토리 프록시 최신화

백엔드 Swagger에 노출되는 AI 프록시 API가 AI repo 최신 엔드포인트에 맞춰 보강됐다.

```http
GET /api/ai/health
POST /api/stories/generate
POST /api/stories/play
GET /api/stories/play/history
GET /api/stories/scenarios
GET /api/stories/scenarios/{scenarioId}
```

숨김 유지:

```http
GET /api/stories/quests/today
GET /api/stories/quests
GET /api/stories/quests/{questId}
POST /api/stories/play/start
POST /api/stories/play/continue
POST /api/stories/play/choose
POST /api/stories/play/next-chapter
```

`POST /api/stories/play`, `GET /api/stories/play/history`는 프론트가 `user_id`를 보내지 않는다. 백엔드가 JWT 로그인 사용자 ID를 AI 서버 요청에 주입한다.

AI 서버에는 프론트의 `Authorization` 헤더를 전달하지 않는다. 인증은 백엔드에서만 처리하고, AI 서버에는 필요한 사용자 식별값으로 `user_id`만 주입한다. 따라서 Swagger의 개별 API 파라미터에 `Authorization string (header)`가 따로 보이면 안 된다.

스토리 퀘스트 프록시 3개는 현재 백엔드/프론트 실제 플로우에서 사용하지 않으므로 Swagger에서 숨긴다. 구현은 남겨두되, 필요성이 확정되기 전까지 프론트 연동 대상 API로 보지 않는다.

AI 프록시 응답은 모두 백엔드 공통 응답 래퍼로 감싼다. Swagger 200 응답 예시는 실제 응답처럼 `data` 아래에 AI 서버 원본 응답이 들어가는 형태로 표시한다.

```json
{
  "success": true,
  "code": "OK",
  "message": "AI 스토리 진행 응답을 조회했습니다.",
  "timestamp": "2026-05-05T18:40:00+09:00",
  "data": {
    "scenario_id": 2,
    "chapter_num": 1,
    "phase": "DETAIL",
    "opening_characters": [],
    "dialogue": [],
    "choices": []
  }
}
```

## 4. 스토리 생성 DTO 보강

`POST /api/stories/generate` 요청 DTO에 AI 서버 최신 필드가 추가됐다.

추가 선택 필드:

```text
representative_character_name
characters[].is_representative
```

예시:

```json
{
  "title": "붉은 달의 계약",
  "genre": "로맨스 판타지",
  "world_setting": "마법과 귀족 정치가 공존하는 제국",
  "tone_and_mood": "긴장감 있고 서정적이다.",
  "player_role": "기억을 잃은 계약자",
  "core_conflict": "플레이어는 예언과 정체성 사이에서 선택해야 한다.",
  "representative_character_name": "리안",
  "chapter_count": 5,
  "characters": [
    {
      "name": "리안",
      "role": "황태자",
      "personality": "차갑고 신중하지만 플레이어에게만 약한 면을 보인다.",
      "relationship_to_player": "처음에는 경계하지만 점점 신뢰하게 되는 인물",
      "background": "제국의 정치적 음모 속에서 살아남은 후계자",
      "special_notes": "플레이어가 위험해질 때 감정이 크게 흔들린다.",
      "is_representative": true
    }
  ]
}
```

## 5. 개발 서버 테스트 데이터

개발 서버 DB에서 `admin123` 계정의 활성 루틴에 화요일 테스트 세션을 추가했다.

```text
loginId: admin123
activeRoutineId: 8
sessionName: 화요일 복합 테스트 세션
dayOfWeek: TUESDAY
estimatedMinutes: 60
```

화요일 세션 카테고리:

```text
근력
유산소
러닝
사이클
스트레칭
요가
수영
```

검증 API:

```http
GET /api/routines/today
Authorization: Bearer {admin123 accessToken}
```

오늘이 화요일이면 `session.dayOfWeek=TUESDAY`와 위 운동 카테고리들이 내려간다.

## 6. 검증 상태

로컬 테스트:

```text
./gradlew test
```

통과.

개발 서버 배포 확인은 직전 배포 기준 `capstone-backend` 새 컨테이너 기동, `/api/health` 200, Swagger에 신규 API 노출까지 확인했다.

## 7. 계정/프로필 API 보강

FCM 푸시 발송은 이번 범위에서 제외했다. 아래 API는 계정 복구, 마이페이지 조회, 사용자 정보 수정, 프로필 설정까지만 다룬다.

### 7.1 회원가입 선택 필드 추가

`POST /api/auth/signup`

기존 필수 필드는 유지하고, 아이디 찾기/비밀번호 재설정에 사용할 선택 필드가 추가됐다.

```json
{
  "loginId": "demoUser",
  "password": "password123",
  "nickname": "데모유저",
  "email": "demo@example.com"
}
```

`email`은 선택값이다. 다만 아이디 찾기/비밀번호 재설정을 쓰려면 가입 또는 사용자 정보 수정에서 이메일이 저장되어 있어야 한다.

### 7.2 아이디 찾기

`POST /api/auth/find-login-id`

이메일로 로그인 아이디 안내 메일을 발송한다. 로그인 아이디는 응답 body로 직접 내려주지 않는다.

```json
{
  "email": "demo@example.com"
}
```

응답 `data`는 `null`이다.

```json
{
  "success": true,
  "code": "OK",
  "message": "아이디 안내 메일을 발송했습니다.",
  "data": null
}
```

### 7.3 비밀번호 재설정 메일 발송

`POST /api/auth/password-reset/request`

가입 이메일로 비밀번호 재설정 토큰과 딥링크를 발송한다.

```json
{
  "email": "demo@example.com"
}
```

메일에는 아래 두 값이 포함된다.

```text
재설정 링크: sweetnsweat://password-reset?token={token}
재설정 토큰: {token}
```

토큰 기본 유효 시간은 30분이다.

### 7.4 비밀번호 재설정 확정

`POST /api/auth/password-reset/confirm`

메일로 받은 토큰을 검증하고 새 비밀번호로 변경한다. 성공하면 기존 refresh token은 폐기된다.

```json
{
  "token": "메일로 받은 토큰",
  "newPassword": "newPassword123"
}
```

### 7.5 마이페이지 조회

`GET /api/users/me/mypage`

마이페이지 상단 프로필 카드와 이번 주 통계에 필요한 값을 한 번에 조회한다.

주요 필드:

```text
id
loginId
nickname
profileImageUrl
level
totalExp
currentLevelExp
nextLevelRequiredExp
nextLevelRemainingExp
balanceCurrency
currentStreakDays
activeRoutineId
activeRoutineName
onboardingCompleted
todayConditionCompleted
routineSetupRequired
weeklyStats
```

`weeklyStats`는 기존 `GET /api/users/me/weekly-stats`와 같은 구조다.

### 7.6 사용자 정보 수정

`PUT /api/users/me`

닉네임, 이메일을 수정한다. 두 필드 중 수정할 값만 보내면 된다.

```json
{
  "nickname": "새닉네임",
  "email": "new@example.com"
}
```

### 7.7 프로필 설정

`PUT /api/users/me/profile`

마이페이지 프로필 카드에 표시할 닉네임과 프로필 이미지 URL을 설정한다.

```json
{
  "nickname": "프로필닉네임",
  "profileImageUrl": "/media/assets/profile-demo.png"
}
```
