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

`GET /api/users/me/routines/active`

`PUT /api/users/me/routines/active`

`POST /api/routines/{routineId}/activate`

위 응답 모두 `RoutineDetailResponse`를 사용하므로 `sessions`가 포함된다.

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
