# 2026-05-06 프론트 전달 사항

오늘 프론트 연동 대상은 스토리 채팅 목록/입장 정보 API와 계정 복구 API 확정 흐름이다.

개발 서버 기준:

```text
Base URL: http://100.89.171.113:8080
Swagger: http://100.89.171.113:8080/swagger-ui/index.html
```

공통 응답은 기존과 동일하게 `ApiResponse` 래퍼로 내려간다.

```json
{
  "success": true,
  "code": "OK",
  "message": "Request succeeded",
  "timestamp": "2026-05-06T00:02:35+09:00",
  "data": {}
}
```

## 1. 스토리 채팅 목록 조회

이미 한 번 이상 입장해서 `story_progress`가 생긴 세계관 채팅방 목록을 조회한다. 채팅 탭의 방 목록 화면에 붙이면 된다.

```http
GET /api/stories/chats?limit=50
Authorization: Bearer {accessToken}
```

쿼리 파라미터:

| 이름 | 필수 | 기본값 | 범위 | 설명 |
| --- | --- | --- | --- | --- |
| `limit` | N | `50` | `1~100` | 최근 업데이트 순으로 가져올 채팅방 수 |

응답 예시:

```json
{
  "success": true,
  "code": "OK",
  "message": "Request succeeded",
  "data": {
    "limit": 50,
    "totalCount": 2,
    "chats": [
      {
        "progressId": 2,
        "scenarioId": 4,
        "scenarioTitle": "월하검귀는 다시 웃지 않는다",
        "worldTitle": "월하검귀는 다시 웃지 않는다",
        "summary": "복수와 회귀를 다루는 무협 세계관",
        "genre": "무협 회귀 복수 로맨스",
        "thumbnailUrl": "http://100.89.171.113:8000/media/assets/thumb.png",
        "worldImageUrl": "http://100.89.171.113:8000/media/assets/world.png",
        "playerImageUrl": "http://100.89.171.113:8000/media/assets/player.png",
        "representativeCharacter": {
          "id": 12,
          "name": "천류하",
          "title": "검귀",
          "type": "main",
          "imageUrl": "http://100.89.171.113:8000/media/assets/character.png",
          "quote": "네 뒤를 쫓아가도...",
          "tags": ["검귀", "복수"]
        },
        "displayName": "천류하",
        "imageUrl": "http://100.89.171.113:8000/media/assets/character.png",
        "backgroundImageUrl": "http://100.89.171.113:8000/media/assets/world.png",
        "status": "IN_PROGRESS",
        "currentChapterNum": 2,
        "phase": "choice",
        "lastMessage": "마지막 AI 응답 요약 또는 원문",
        "startedAt": "2026-05-05T10:00:00Z",
        "updatedAt": "2026-05-05T11:10:00Z",
        "historyEndpoint": "/api/stories/play/history?scenario_id=4",
        "playEndpoint": "/api/stories/play"
      }
    ]
  }
}
```

프론트 사용 기준:

- 채팅방 카드 제목은 `displayName` 우선 사용, 없으면 `scenarioTitle` 사용.
- 카드 이미지는 `imageUrl` 사용.
- 카드 배경이 필요하면 `backgroundImageUrl` 사용.
- 마지막 대화 미리보기는 `lastMessage` 사용.
- `historyEndpoint`, `playEndpoint`는 프론트 참고용이다. 실제 호출은 기존 AI 프록시 API 규칙을 그대로 따르면 된다.

## 2. 스토리 채팅방 입장 정보 조회

채팅 목록에서 특정 방을 눌렀을 때, 채팅방 상단 메타데이터/캐릭터 목록/최근 대화 일부를 한 번에 조회한다.

```http
GET /api/stories/chats/{scenarioId}?messageLimit=30
Authorization: Bearer {accessToken}
```

경로/쿼리 파라미터:

| 이름 | 필수 | 기본값 | 범위 | 설명 |
| --- | --- | --- | --- | --- |
| `scenarioId` | Y | - | `1 이상` | 입장할 세계관 ID |
| `messageLimit` | N | `30` | `1~100` | 함께 내려받을 최근 대화 턴 수 |

응답 예시:

```json
{
  "success": true,
  "code": "OK",
  "message": "Request succeeded",
  "data": {
    "chat": {
      "progressId": 2,
      "scenarioId": 4,
      "scenarioTitle": "월하검귀는 다시 웃지 않는다",
      "displayName": "천류하",
      "imageUrl": "http://100.89.171.113:8000/media/assets/character.png",
      "backgroundImageUrl": "http://100.89.171.113:8000/media/assets/world.png",
      "status": "IN_PROGRESS",
      "currentChapterNum": 2,
      "phase": "choice",
      "lastMessage": "마지막 AI 응답"
    },
    "characters": [
      {
        "id": 12,
        "name": "천류하",
        "title": "검귀",
        "type": "main",
        "imageUrl": "http://100.89.171.113:8000/media/assets/character.png",
        "quote": "네 뒤를 쫓아가도...",
        "tags": ["검귀", "복수"],
        "representative": true
      }
    ],
    "messageLimit": 30,
    "messageTotalCount": 8,
    "hasMoreMessages": false,
    "recentMessages": [
      {
        "id": 101,
        "chapterNum": 1,
        "choiceId": 37,
        "detailId": null,
        "unitIndex": 3,
        "userMessage": "주변을 둘러본다.",
        "narrationText": "낯선 공기가 폐부 깊숙이 스며든다.",
        "dialogueText": "여기가 어디인지 정말 모르는 건가?",
        "outputText": "AI 서버 원본 응답 텍스트",
        "createdAt": "2026-05-05T10:03:00Z"
      }
    ]
  }
}
```

예외:

```text
401 UNAUTHORIZED
- 토큰이 없거나 만료됨

404 STORY_CHAT_NOT_FOUND
- 로그인 사용자가 해당 세계관에 입장한 기록이 없음
- 비활성 세계관임
```

## 3. 채팅방 화면에서 이어서 대화하는 방법

채팅방 입장 정보 API는 화면 구성용 데이터만 내려준다. 실제 대화 진행은 기존 AI 프록시를 그대로 사용한다.

대화 진행:

```http
POST /api/stories/play
Authorization: Bearer {accessToken}
Content-Type: application/json
```

요청 예시:

```json
{
  "scenario_id": 4,
  "user_message": "주변을 둘러본다.",
  "choice_id": 37,
  "restart": false
}
```

주의:

- 프론트는 `user_id`를 보내지 않는다.
- 백엔드가 JWT 사용자 ID를 AI 서버 요청에 주입한다.
- 백엔드 AI 프록시는 프론트가 보낸 기존 `Authorization: Bearer {accessToken}` 헤더를 AI 서버 호출에도 그대로 전달한다.
- 새로 시작하기는 AI 담당 기준에 맞춰 `restart: true`로 호출한다.
- 기존 대화 이어하기는 `restart: false`로 호출한다.

대화 히스토리 직접 조회가 필요하면:

```http
GET /api/stories/play/history?scenario_id=4&limit=30
Authorization: Bearer {accessToken}
```

## 4. 계정 복구 API 확정

계정 복구는 이메일만 사용한다. 휴대폰 기반 복구는 구현하지 않는다.

### 4.1 아이디 찾기

```http
POST /api/auth/find-login-id
Content-Type: application/json
```

요청:

```json
{
  "loginId": "demoUser",
  "email": "demo@example.com"
}
```

응답:

```json
{
  "success": true,
  "code": "OK",
  "message": "아이디 안내 메일을 발송했습니다.",
  "data": null
}
```

로그인 아이디는 응답으로 직접 내려주지 않고 메일로 발송한다.

### 4.2 임시 비밀번호 발급

```http
POST /api/auth/password-reset/request
Content-Type: application/json
```

요청:

```json
{
  "email": "demo@example.com"
}
```

응답:

```json
{
  "success": true,
  "code": "OK",
  "message": "임시 비밀번호를 이메일로 발송했습니다.",
  "data": null
}
```

동작:

- 백엔드가 10자리 임시 비밀번호를 생성한다.
- `loginId`와 `email`이 같은 계정에 등록된 정보인지 확인한다.
- 기존 비밀번호는 즉시 임시 비밀번호로 변경된다.
- 기존 refresh token은 폐기된다.
- 메일에는 임시 비밀번호만 포함된다.
- 재설정 링크나 긴 토큰은 없다.

메일 예시:

```text
임시 비밀번호: Ab3xYp8Qw2
```

프론트 플로우:

1. 사용자가 이메일 입력
2. `POST /api/auth/password-reset/request`
3. 성공 시 “메일로 받은 임시 비밀번호로 로그인해 주세요.” 안내
4. 로그인 화면으로 이동

제거된 API:

```http
POST /api/auth/password-reset/confirm
```

이 API는 더 이상 Swagger에 노출되지 않는다.

## 5. 닉네임 중복 체크

회원가입 화면에서 닉네임 중복 확인용으로 사용한다.

```http
GET /api/auth/nickname/check?nickname=수연
```

응답:

```json
{
  "success": true,
  "code": "OK",
  "message": "닉네임 사용 가능 여부를 조회했습니다.",
  "data": {
    "nickname": "수연",
    "available": true,
    "duplicated": false
  }
}
```

예외:

```text
400 NICKNAME_REQUIRED
- nickname이 비어 있음

400 INVALID_NICKNAME
- nickname이 2자 미만 또는 50자 초과
```

## 6. 마이페이지/계정 수정 관련

### 6.1 마이페이지 조회

```http
GET /api/users/me/mypage
Authorization: Bearer {accessToken}
```

마이페이지 상단 카드와 주간 통계 요약을 한 번에 조회한다.

현재 이 API는 구현은 유지하지만 Swagger에서는 숨김 처리했다. 프론트에서 필요하면 위 경로로 직접 호출하면 된다.

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

### 6.2 사용자 정보 수정

```http
PUT /api/users/me
Authorization: Bearer {accessToken}
Content-Type: application/json
```

닉네임, 이메일, 성별, 키, 몸무게를 수정한다. 수정할 필드만 보내면 되고 휴대폰 번호는 받지 않는다.

```json
{
  "nickname": "새닉네임",
  "email": "new@example.com",
  "gender": "female",
  "heightCm": 164.5,
  "weightKg": 58.2
}
```

### 6.3 프로필 설정

```http
PUT /api/users/me/profile
Authorization: Bearer {accessToken}
Content-Type: application/json
```

마이페이지 프로필 카드에 표시할 닉네임과 프로필 이미지 URL을 설정한다.

현재 이 API는 구현은 유지하지만 Swagger에서는 숨김 처리했다. 프론트에서 필요하면 위 경로로 직접 호출하면 된다.

```json
{
  "nickname": "프로필닉네임",
  "profileImageUrl": "/media/assets/profile-demo.png"
}
```

## 7. 배포/검증 상태

병합된 커밋:

```text
9cd207a feat: add story chat list APIs
915f11d fix: issue temporary password for reset email
f828f4a feat: send account recovery emails
```

검증:

```text
./gradlew clean test
```

통과.

개발 서버 확인:

```text
GET /api/health -> 200 OK
Swagger 노출:
- /api/stories/chats
- /api/stories/chats/{scenarioId}
```
