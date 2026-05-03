# 2026-05-01 프론트 개발 연동 정리

## 목적

오늘 백엔드에 추가된 온보딩, 루틴 추천, 루틴 활성화, 운동 목록, 컨디션, 퀘스트 흐름을 프론트에서 막히지 않고 붙일 수 있도록 정리한다.

기준 시간은 모두 KST다.

## 전체 사용자 흐름

```text
회원가입/로그인
-> GET /api/users/me
-> 온보딩 미완료면 PUT /api/users/me/onboarding-profile
-> 온보딩 완료 직후 activeRoutineId가 없으면 루틴 설정 방식 선택
   1. 추천 루틴 받기
      -> GET /api/routines/recommendations
      -> 사용자가 추천 루틴 선택
      -> POST /api/routines/{routineId}/activate
   2. 내 루틴 직접 만들기
      -> 운동 목록 조회 후 커스텀 루틴 생성 화면으로 이동
      -> POST /api/routines/custom
   3. 나중에 할게요
      -> 홈 진입 가능
      -> activeRoutineId가 없으므로 퀘스트 생성 시 루틴 설정 필요 응답 발생
-> GET /api/users/me/routines/active
-> 스토리/퀘스트 진입 시 오늘 컨디션 미입력이면 PUT /api/conditions/today
-> 스토리/AI 퀘스트 분기에서 GET /api/quests/today
-> 수행 완료 시 PATCH /api/quests/{questId}/complete
```

## 로그인 후 내 상태 확인

```http
GET /api/users/me
Authorization: Bearer {accessToken}
```

프론트가 봐야 하는 핵심 필드:

```json
{
  "id": 1,
  "loginId": "demo_gym_quest",
  "nickname": "데모 헬스장 퀘스트",
  "onboardingCompleted": true,
  "todayConditionCompleted": true,
  "activeRoutineId": 6,
  "routineSetupRequired": false,
  "experienceLevel": "beginner",
  "currentExerciseStatus": "occasional",
  "fitnessGoal": "strength",
  "preferredWorkoutPlace": "gym",
  "weeklyWorkoutFrequency": 3,
  "availableWorkoutMinutes": 35,
  "preferredExerciseTypes": ["strength", "cardio"]
}
```

프론트 분기:

```text
onboardingCompleted=false -> 온보딩 화면
routineSetupRequired=true -> 온보딩 직후에는 루틴 설정 방식 선택 화면
todayConditionCompleted=false -> 스토리/퀘스트 진입 시 오늘 컨디션 입력 화면
activeRoutineId가 있으면 홈/스토리/퀘스트 화면 진입 가능
```

`routineSetupRequired=true`는 홈 진입을 막는 하드 블로킹 값이 아니다.

사용자가 `나중에 할게요`를 선택했다면 홈으로 보낼 수 있다. 다만 activeRoutineId가 없기 때문에 퀘스트 생성은 불가능하고, 스토리에서 퀘스트 분기 진입 전 루틴 설정 CTA를 다시 보여주는 식으로 처리한다.

활성 루틴이 없을 때 프론트 분기:

```text
추천 루틴 받기 선택
-> GET /api/routines/recommendations
-> POST /api/routines/{routineId}/activate

내 루틴 직접 만들기 선택
-> GET /api/exercises/categories
-> GET /api/exercises
-> 사용자가 운동을 고르는 화면 표시
-> POST /api/routines/custom
-> 생성한 루틴이 기본적으로 바로 활성 루틴으로 설정됨

나중에 할게요 선택
-> 별도 API 호출 없음
-> GET /api/users/me에서 activeRoutineId는 계속 null, routineSetupRequired는 true
-> GET /api/quests/today 호출 시 ACTIVE_ROUTINE_REQUIRED 응답
```

현재 구현 완료된 루틴 설정 방식은 `추천 루틴 받기`와 `내 루틴 직접 만들기`다.

`내 루틴 직접 만들기`는 운동 목록 화면에서 선택 모드로 진입한 뒤, 선택한 운동들을 `POST /api/routines/custom`으로 저장한다.

최초 온보딩 저장 시 오늘 컨디션은 백엔드가 기본값으로 자동 생성한다.

기본값은 conditionLevel=3, sleepScore=3, stressScore=2, energyLevel=3이며 conditionScore=60.42, exerciseMultiplier=1.00이다.

따라서 회원가입 직후에는 컨디션 입력 화면을 바로 강제하지 않는다. 사용자가 나중에 스토리/퀘스트에 진입했을 때 오늘 컨디션이 없으면 그때 입력받으면 된다.

## 온보딩 저장

```http
PUT /api/users/me/onboarding-profile
Authorization: Bearer {accessToken}
Content-Type: application/json
```

요청:

```json
{
  "gender": "female",
  "birthDate": "2002-05-20",
  "heightCm": 164.5,
  "weightKg": 58.2,
  "experienceLevel": "beginner",
  "currentExerciseStatus": "none",
  "fitnessGoal": "habit",
  "preferredWorkoutPlace": "home",
  "weeklyWorkoutFrequency": 3,
  "availableWorkoutMinutes": 30,
  "preferredExerciseTypes": ["bodyweight", "cardio"]
}
```

허용값:

```text
gender: male, female, prefer_not_to_say
experienceLevel: beginner, intermediate, advanced
currentExerciseStatus: none, occasional, regular
fitnessGoal: stamina, weight_loss, strength, habit, stress_relief
preferredWorkoutPlace: home, gym, outdoor, facility, other
weeklyWorkoutFrequency: 1~7
availableWorkoutMinutes: 10~180
preferredExerciseTypes: strength, cardio, stretching, bodyweight, walking, running, swimming, yoga_pilates
```

`preferredExerciseTypes`는 빈 배열 가능. 선호 운동을 고르지 않은 경우 아래처럼 보내면 된다.

```json
{
  "preferredExerciseTypes": []
}
```

필드 자체를 생략하거나 `null`로 보내도 백엔드 저장 시 빈 배열로 처리된다. 다만 프론트 상태 관리는 빈 배열 `[]`로 통일하는 것을 권장한다.

## 오늘 컨디션 저장

```http
PUT /api/conditions/today
Authorization: Bearer {accessToken}
Content-Type: application/json
```

요청:

```json
{
  "conditionLevel": 4,
  "sleepScore": 3,
  "stressScore": 2,
  "energyLevel": 4
}
```

응답 핵심:

```json
{
  "logDate": "2026-05-01",
  "conditionLevel": 4,
  "sleepScore": 3,
  "stressScore": 2,
  "energyLevel": 4,
  "conditionScore": 72.92,
  "exerciseMultiplier": 1.00
}
```

퀘스트 생성은 오늘 컨디션이 있어야 가능하다.

## 온보딩 기반 루틴 추천

이 API는 사용자가 `추천 루틴 받기`를 선택했을 때만 호출한다.

`내 루틴 직접 만들기`를 선택한 경우 이 API를 호출하지 않고 운동 목록 화면으로 이동한다.

```http
GET /api/routines/recommendations
Authorization: Bearer {accessToken}
```

응답:

```json
{
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
        "현재 운동 상태에 맞는 시작 강도입니다."
      ]
    }
  ]
}
```

추천은 기본 루틴 중 1~2개가 내려간다.

## 직접 루틴 생성

이 API는 사용자가 `제가 알아서 만들래요`를 선택한 뒤, 운동 목록에서 운동을 고르고 루틴 이름/요일/세트/횟수를 입력했을 때 호출한다.

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
          "durationSec": 45,
          "restSec": 30
        }
      ]
    }
  ]
}
```

`activate`를 생략하거나 `true`로 보내면 저장한 루틴이 바로 활성 루틴으로 설정된다.

`items[].sets`, `items[].reps`, `items[].durationSec` 중 하나 이상은 필요하다.

## 추천 루틴 선택 및 활성화

추천 카드에서 사용자가 선택하면 이 API를 호출한다.

```http
POST /api/routines/{routineId}/activate
Authorization: Bearer {accessToken}
```

중요:

```text
기본 루틴을 직접 활성화하지 않는다.
백엔드가 사용자 전용 루틴으로 복사한 뒤 그 복사본을 활성화한다.
같은 기본 루틴을 다시 선택하면 기존 복사본을 재사용한다.
```

응답:

```json
{
  "id": 6,
  "name": "초급 헬스장 근력 입문 루틴",
  "description": "헬스장에서 머신과 가벼운 유산소로 시작하는 초급 근력 루틴입니다.",
  "difficulty": "easy",
  "estimatedMinutes": 30,
  "isDefault": false,
  "sourceRoutineId": 4,
  "sessions": [
    {
      "id": 16,
      "seq": 1,
      "dayOfWeek": "MONDAY",
      "dayOfWeekDisplayName": "월요일",
      "sessionName": "상체 머신",
      "sessionType": "upper_body",
      "sessionTypeDisplayName": "상체",
      "estimatedMinutes": 30,
      "active": true,
      "items": []
    }
  ],
  "items": []
}
```

`id`는 사용자 복사본 루틴 ID다.

`sourceRoutineId`는 원본 기본 루틴 ID다.

## 내 루틴 목록 조회

```http
GET /api/users/me/routines
Authorization: Bearer {accessToken}
```

직접 만든 루틴과 추천 루틴 선택으로 복사된 사용자 루틴 목록을 조회한다.

```json
{
  "data": [
    {
      "id": 20,
      "name": "초급 헬스장 근력 입문 루틴",
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

`active=true`인 항목이 현재 활성 루틴이다.

## 활성 루틴 조회

```http
GET /api/users/me/routines/active
Authorization: Bearer {accessToken}
```

요일별 루틴 화면은 `sessions`를 사용하면 된다.

```text
sessions[].dayOfWeek
sessions[].dayOfWeekDisplayName
sessions[].sessionName
sessions[].items[]
```

기존 호환용 `items`도 남아 있지만, 앞으로는 `sessions[].items`를 우선 사용한다.

## 운동 목록 조회

```http
GET /api/exercises?scope=all&category=헬스&level=초급&keyword=스쿼트&page=0&size=50
Authorization: Bearer {accessToken}
```

쿼리:

```text
scope: all, favorite, recent
category: 전체, 수영, 요가, 러닝, 사이클, 헬스, 필라테스
level: 전체, 입문, 초급, 중급, 고급
keyword: 운동명 검색
page: 0부터 시작
size: 1~100
```

응답 핵심:

```json
{
  "totalCount": 16,
  "groups": [
    {
      "category": "헬스",
      "items": [
        {
          "id": 10,
          "name": "Chest Press",
          "category": "근력",
          "categoryDisplayName": "헬스",
          "level": "초급",
          "levelDisplayName": "초급",
          "estimatedKcalPerHour": 300,
          "emoji": "🏋️",
          "liked": true
        }
      ]
    }
  ]
}
```

칼로리는 로그인 사용자의 몸무게와 MET 기준으로 계산된다.

몸무게가 없으면 성별 평균 체중, 그것도 없으면 70kg 기준이다.

## 운동 좋아요

```http
PUT /api/users/me/exercises/{exerciseId}/favorite
Authorization: Bearer {accessToken}
Content-Type: application/json
```

요청:

```json
{
  "liked": true
}
```

해제:

```json
{
  "liked": false
}
```

좋아요 목록:

```http
GET /api/exercises?scope=favorite
```

## 오늘 퀘스트 조회 및 자동 생성

```http
GET /api/quests/today
Authorization: Bearer {accessToken}
```

프론트/AI는 퀘스트 생성 API를 따로 호출하지 않는다.

동작:

```text
오늘 퀘스트 있음 -> 그대로 반환
오늘 퀘스트 없음 -> 활성 루틴 + 오늘 요일 세션 + 오늘 컨디션 기준으로 자동 생성
같은 날 여러 번 호출 -> 같은 퀘스트 반환
완료 후 다시 호출 -> COMPLETED 상태의 같은 퀘스트 반환
지난 날짜 미완료 퀘스트 -> EXPIRED 처리
```

응답:

```json
{
  "id": 12,
  "questDate": "2026-05-01",
  "questType": "ROUTINE",
  "targetMetric": "EXERCISES",
  "status": "ISSUED",
  "completed": false,
  "title": "상체 머신 운동 2개 완료",
  "description": "상체 머신 세션에서 제시된 운동 2개를 완료해 주세요.",
  "targetValue": 2,
  "progressValue": 0,
  "conditionAdjusted": false,
  "routineId": 6,
  "routineName": "초급 헬스장 근력 입문 루틴",
  "sourceSessionId": 16,
  "sessionName": "상체 머신",
  "sessionType": "upper_body",
  "conditionScore": 72.92,
  "exerciseMultiplier": 1.00,
  "rewardCurrency": 30,
  "rewardExp": 20,
  "completedAt": null,
  "exercises": [
    {
      "exerciseId": 10,
      "exerciseName": "Chest Press",
      "category": "strength",
      "seq": 1,
      "targetSets": 3,
      "targetReps": 10,
      "targetDurationSec": null
    }
  ]
}
```

퀘스트 타입:

```text
ROUTINE: 오늘 요일에 루틴 세션이 있음
OFF_DAY: 오늘 요일에 루틴 세션이 없음
RECOVERY: 컨디션이 낮아서 회복 퀘스트로 대체
```

## 퀘스트 완료

```http
PATCH /api/quests/{questId}/complete
Authorization: Bearer {accessToken}
Content-Type: application/json
```

요청 body는 생략 가능하다.

```json
{
  "progressValue": 2,
  "proof": {
    "source": "manual"
  }
}
```

완료 후:

```json
{
  "status": "COMPLETED",
  "completed": true,
  "progressValue": 2
}
```

## AI 서버 연동

프론트가 스토리 플레이 API를 호출할 때 `user_id`는 보내지 않는다.

백엔드가 JWT에서 로그인 사용자 ID를 꺼내 AI 서버 요청의 `user_id`로 주입한다. Swagger에는 `scenario_id`, `user_message`, `choice_id`, `restart`가 요청 필드로 표시된다.

```http
POST /api/stories/play
Authorization: Bearer {accessToken}
Content-Type: application/json
```

요청:

```json
{
  "scenario_id": 4,
  "user_message": "카이렌을 바라보며 지금 어디로 가야 하는지 묻는다.",
  "choice_id": null,
  "restart": false
}
```

처음 세계관에 입장하거나 처음부터 다시 시작할 때:

```json
{
  "scenario_id": 4,
  "restart": true
}
```

백엔드가 AI 서버로 보낼 때:

```json
{
  "user_id": 11,
  "scenario_id": 4,
  "user_message": "카이렌을 바라보며 지금 어디로 가야 하는지 묻는다.",
  "choice_id": null,
  "restart": false
}
```

프론트가 실수로 `user_id`를 보내도 요청 DTO에서는 사용하지 않고, 백엔드 인증 사용자 ID만 AI 서버로 전달한다.

잘못된 JSON 본문을 보내면:

```json
{
  "code": "INVALID_REQUEST_BODY",
  "detail": "요청 본문 JSON 형식이 올바르지 않습니다."
}
```

AI 서버는 DB를 직접 보지 말고 백엔드 API를 호출한다.

스토리 진행 중 퀘스트 분기를 만나면:

```text
AI 서버 -> GET /api/quests/today
백엔드 -> 오늘 퀘스트 원본 반환
AI 서버 -> title, description, exercises를 세계관에 맞게 래핑
AI 서버 -> 프론트에 스토리 응답
```

퀘스트 중복 생성 방지와 완료 여부 판단은 백엔드가 담당한다.

## 자주 나올 예외

온보딩 필요:

```json
{
  "code": "ONBOARDING_REQUIRED",
  "detail": "퀘스트를 생성하려면 온보딩 프로필을 먼저 저장해 주세요."
}
```

오늘 컨디션 필요:

```json
{
  "code": "CONDITION_REQUIRED",
  "detail": "오늘 퀘스트를 생성하려면 먼저 오늘 컨디션을 입력해 주세요."
}
```

활성 루틴 필요:

```json
{
  "code": "ACTIVE_ROUTINE_REQUIRED",
  "detail": "오늘 퀘스트를 생성하려면 추천 루틴을 선택하거나 내 루틴을 먼저 만들어 주세요."
}
```

지난 날짜 퀘스트 완료 불가:

```json
{
  "code": "QUEST_EXPIRED",
  "detail": "지난 날짜의 미완료 퀘스트는 완료할 수 없습니다."
}
```

## 더미 계정

로컬 DB와 개발 서버 DB에 동일하게 들어가 있다.

비밀번호는 모두 `password123`.

```text
demo_recommend
- 온보딩 완료
- 활성 루틴 없음
- 루틴 추천 API 테스트용

demo_gym_quest
- 온보딩 완료
- 헬스장 루틴 복사/활성화 완료
- 오늘 컨디션 있음
- GET /api/quests/today 호출 시 ROUTINE 퀘스트 테스트 가능

demo_recovery_quest
- 온보딩 완료
- 홈트 루틴 복사/활성화 완료
- 오늘 낮은 컨디션 있음
- GET /api/quests/today 호출 시 RECOVERY 퀘스트 테스트 가능
```

## 추가 참고 문서

- `ONBOARDING_API_FRONTEND_GUIDE.md`
- `ROUTINE_API_FRONTEND_GUIDE.md`
- `EXERCISE_API_FRONTEND_GUIDE.md`
- `QUEST_API_FRONTEND_GUIDE.md`
- `FRONTEND_HANDOFF_20260502.md`
