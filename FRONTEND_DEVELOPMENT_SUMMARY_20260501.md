# 2026-05-01 프론트 개발 연동 정리

## 목적

오늘 백엔드에 추가된 온보딩, 루틴 추천, 루틴 활성화, 운동 목록, 컨디션, 퀘스트 흐름을 프론트에서 막히지 않고 붙일 수 있도록 정리한다.

기준 시간은 모두 KST다.

## 전체 사용자 흐름

```text
회원가입/로그인
-> GET /api/users/me
-> 온보딩 미완료면 PUT /api/users/me/onboarding-profile
-> 오늘 컨디션 미입력이면 PUT /api/conditions/today
-> GET /api/routines/recommendations
-> 사용자가 추천 루틴 선택
-> POST /api/routines/{routineId}/activate
-> GET /api/users/me/routines/active
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
todayConditionCompleted=false -> 오늘 컨디션 입력 화면
activeRoutineId=null -> 루틴 추천/선택 화면
전부 충족 -> 홈/스토리/퀘스트 화면 진입 가능
```

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
fitnessGoal: habit, strength, stamina, diet, flexibility
preferredWorkoutPlace: home, gym, outdoor, other
weeklyWorkoutFrequency: 1~7
availableWorkoutMinutes: 10~180
preferredExerciseTypes: bodyweight, strength, cardio, running, cycling, swimming, yoga, pilates, mobility
```

`preferredExerciseTypes`는 빈 배열 가능.

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
  "detail": "오늘 퀘스트를 생성하려면 먼저 활성 루틴을 설정해 주세요."
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
- `CONDITION_SCORE_MODEL.md`
