# 루틴 상세 API 세션 응답

루틴 상세 응답에 기존 `items` flat list를 유지하면서, 요일별 루틴 화면과 퀘스트 생성을 위한 `sessions` 구조를 추가했다.

## 온보딩 기반 루틴 추천

`GET /api/routines/recommendations`

인증: `Authorization: Bearer {accessToken}`

현재 로그인 사용자의 온보딩 값을 기준으로 기본 루틴 중 상위 1~2개를 추천한다.

추천 기준:

```text
experienceLevel
currentExerciseStatus
fitnessGoal
preferredWorkoutPlace
weeklyWorkoutFrequency
availableWorkoutMinutes
preferredExerciseTypes
```

응답 예시:

```json
{
  "success": true,
  "code": "OK",
  "message": "Request succeeded",
  "timestamp": "2026-05-01T18:25:00+09:00",
  "data": [
    {
      "routine": {
        "id": 1,
        "name": "초급 전신 루틴",
        "description": "운동을 처음 시작하는 사용자를 위한 맨몸 전신 루틴입니다.",
        "difficulty": "easy",
        "estimatedMinutes": 25,
        "targetExperienceLevel": "beginner",
        "targetCurrentExerciseStatuses": ["none", "occasional"],
        "goalTypes": ["habit", "stamina", "strength"],
        "placeTypes": ["home", "other"],
        "weeklyFrequency": 3,
        "recommendedExerciseTypes": ["bodyweight", "strength"],
        "isDefault": true
      },
      "score": 115,
      "reasons": [
        "운동 경험 수준이 맞는 루틴입니다.",
        "현재 운동 상태에 맞는 시작 강도입니다.",
        "운동 목표와 잘 맞습니다."
      ]
    }
  ]
}
```

온보딩이 아직 완료되지 않은 사용자가 호출하면 `400 ONBOARDING_REQUIRED`가 내려간다.

## API

`GET /api/routines/{routineId}`

`GET /api/users/me/routines`

`GET /api/users/me/routines/active`

`PUT /api/users/me/routines/active`

`POST /api/routines/{routineId}/activate`

`POST /api/routines/custom`

위 응답 모두 `RoutineDetailResponse`를 사용하므로 `sessions`가 포함된다.

단, `GET /api/users/me/routines`는 목록 화면용 `RoutineSummaryResponse[]`를 반환한다.

## 추천 루틴 선택 및 활성화

추천 루틴 카드에서 사용자가 루틴을 선택하면 아래 API를 호출한다.

```http
POST /api/routines/{routineId}/activate
Authorization: Bearer {accessToken}
```

동작:

```text
기본 루틴(isDefault=true)이면 사용자 전용 루틴으로 복사한 뒤 활성화
이미 같은 기본 루틴을 복사한 적 있으면 기존 사용자 복사본 재사용
사용자 루틴(isDefault=false)이면 해당 사용자 소유 루틴만 그대로 활성화
다른 사용자의 루틴은 활성화 불가
```

응답의 `id`는 실제 활성화된 사용자 루틴 ID다. 기본 루틴을 선택한 경우 원본 기본 루틴 ID가 아니라 복사본 ID가 내려간다.

원본 기본 루틴 ID는 `sourceRoutineId`로 내려간다.

```json
{
  "id": 20,
  "name": "초급 헬스장 근력 입문 루틴",
  "isDefault": false,
  "sourceRoutineId": 4,
  "sessions": []
}
```

## 내 루틴 목록 조회

```http
GET /api/users/me/routines
Authorization: Bearer {accessToken}
```

현재 로그인 사용자가 소유한 루틴만 내려간다.

포함 대상:

```text
직접 만든 루틴
추천 루틴을 선택하면서 사용자 전용으로 복사된 루틴
```

미포함 대상:

```text
전체 기본 루틴
다른 사용자의 개인 루틴
```

응답 예시:

```json
{
  "data": [
    {
      "id": 20,
      "name": "초급 헬스장 근력 입문 루틴",
      "description": "헬스장에서 머신과 가벼운 유산소로 시작하는 초급 근력 루틴입니다.",
      "difficulty": "easy",
      "estimatedMinutes": 30,
      "isDefault": false,
      "sourceRoutineId": 4,
      "active": true
    },
    {
      "id": 21,
      "name": "내 전신 루틴",
      "difficulty": "custom",
      "estimatedMinutes": 40,
      "isDefault": false,
      "sourceRoutineId": null,
      "active": false
    }
  ]
}
```

`active=true`인 루틴이 현재 `GET /api/users/me`의 `activeRoutineId`와 같은 루틴이다.

## 직접 루틴 생성 및 활성화

운동 목록 화면에서 사용자가 `루틴 생성하기`를 누르면 선택 모드로 전환하고, 선택한 운동들로 직접 루틴을 만든다.

```text
운동 목록
-> 루틴 생성하기
-> 운동 여러 개 선택
-> 루틴 이름 / 요일 / 세트 / 횟수 / 시간 입력
-> POST /api/routines/custom
-> activate=true면 생성한 루틴이 바로 활성 루틴으로 설정됨
```

```http
POST /api/routines/custom
Authorization: Bearer {accessToken}
Content-Type: application/json
```

요청:

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
        },
        {
          "exerciseId": 2,
          "seq": 2,
          "durationSec": 45,
          "restSec": 30
        }
      ]
    }
  ]
}
```

필수/검증:

```text
name 필수
sessions 최소 1개
sessions[].dayOfWeek: MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
sessions[].sessionName 필수
sessions[].items 최소 1개
items[].exerciseId는 실제 운동 ID여야 함
items[].sets, items[].reps, items[].durationSec 중 하나 이상 필요
activate 생략 시 true
```

응답은 `RoutineDetailResponse`다. 저장 직후 활성화된 경우 응답의 `id`가 곧 `GET /api/users/me`의 `activeRoutineId`가 된다.

주요 실패:

```text
400 INVALID_ROUTINE_ITEM_TARGET: 세트 수, 반복 횟수, 운동 시간 중 하나도 입력하지 않음
404 EXERCISE_NOT_FOUND: 없는 운동 ID를 루틴에 넣으려고 함
```

## 응답 구조

```json
{
  "id": 4,
  "name": "초급 헬스장 근력 입문 루틴",
  "description": "헬스장에서 머신과 가벼운 유산소로 시작하는 초급 근력 루틴입니다.",
  "difficulty": "easy",
  "estimatedMinutes": 30,
  "targetExperienceLevel": "beginner",
  "targetCurrentExerciseStatuses": ["none", "occasional"],
  "goalTypes": ["strength", "stamina", "habit"],
  "placeTypes": ["gym"],
  "weeklyFrequency": 3,
  "recommendedExerciseTypes": ["strength", "cardio"],
  "isDefault": true,
  "sourceRoutineId": null,
  "sessions": [
    {
      "id": 10,
      "seq": 1,
      "dayOfWeek": "MONDAY",
      "dayOfWeekDisplayName": "월요일",
      "sessionName": "상체 머신",
      "sessionType": "upper_body",
      "sessionTypeDisplayName": "상체",
      "estimatedMinutes": 30,
      "active": true,
      "items": [
        {
          "id": 19,
          "seq": 2,
          "reps": 12,
          "sets": 2,
          "durationSec": null,
          "restSec": 45,
          "exercise": {
            "id": 109,
            "name": "나비",
            "category": "근력",
            "level": "초급",
            "equipment": "머신"
          }
        }
      ]
    }
  ],
  "items": []
}
```

## 프론트 사용 기준

요일별 루틴 화면은 `sessions`를 사용한다.

기존 호환용 단순 운동 목록은 `items`를 사용할 수 있지만, 앞으로는 `sessions[].items`를 우선 사용하면 된다.

퀘스트 생성 기준도 `sessions`다.

```text
오늘 요일과 일치하는 sessions[].dayOfWeek 찾기
-> 해당 session의 items에서 퀘스트 후보 추출
```

요일 표시값은 백엔드가 `dayOfWeekDisplayName`으로 같이 내려준다.

세션 타입 표시값도 백엔드가 `sessionTypeDisplayName`으로 같이 내려준다.
