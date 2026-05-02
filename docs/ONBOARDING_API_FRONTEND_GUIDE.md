# 온보딩 프로필 API 변경사항

프론트 온보딩 화면에서 받은 신체 정보와 루틴 추천용 운동 성향을 저장합니다. 저장된 값은 이후 기본 루틴 추천, 커스텀 루틴 생성 유도, 일일 퀘스트 생성 기준으로 사용합니다.

## API

`PUT /api/users/me/onboarding-profile`

인증: `Authorization: Bearer {accessToken}`

성공 메시지: `온보딩 프로필이 저장되었습니다.`

## 요청 Body

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
  "preferredExerciseTypes": ["bodyweight", "walking"]
}
```

## 필드 설명

| 필드 | 필수 | 허용값/범위 | 화면 의미 | 추천 활용 |
| --- | --- | --- | --- | --- |
| `gender` | O | `male`, `female`, `prefer_not_to_say` | 성별 | 사용자 기본 신체 정보 |
| `birthDate` | O | `YYYY-MM-DD`, 과거 날짜 | 생년월일 | 사용자 기본 신체 정보 |
| `heightCm` | O | `50.0` - `250.0` | 키(cm) | 사용자 기본 신체 정보 |
| `weightKg` | O | `20.0` - `300.0` | 몸무게(kg) | 사용자 기본 신체 정보 |
| `experienceLevel` | O | `beginner`, `intermediate`, `advanced` | 운동 경험 수준 | 초보자 기본 루틴 우선 추천 여부 |
| `currentExerciseStatus` | O | `none`, `occasional`, `regular` | 현재 하고 있는 운동 여부 | 기본 루틴 추천 또는 커스텀 루틴 유도 |
| `fitnessGoal` | O | `stamina`, `weight_loss`, `strength`, `habit`, `stress_relief` | 운동 목표 | 루틴 목적 매칭 |
| `preferredWorkoutPlace` | O | `home`, `gym`, `outdoor`, `facility`, `other` | 주 운동 장소 | 홈트/헬스장/야외/시설 루틴 매칭 |
| `weeklyWorkoutFrequency` | O | `1` - `7` | 주당 운동 가능 횟수 | 주간 루틴 요일/세션 수 결정 |
| `availableWorkoutMinutes` | O | `10` - `180` | 1회 운동 가능 시간 | 루틴 길이와 운동 개수 결정 |
| `preferredExerciseTypes` | X | 최대 10개, 아래 허용값 참고 | 선호 운동 유형 | 선택 시 루틴 우선순위 보정 |

`preferredExerciseTypes` 허용값:

```text
strength
cardio
stretching
bodyweight
walking
running
swimming
yoga_pilates
```

초보자는 선호 운동을 모를 수 있으므로 `preferredExerciseTypes`는 빈 배열로 보내도 됩니다.

## 응답에서 추가된 필드

`POST /api/auth/login`, `GET /api/users/me`, `PUT /api/users/me/onboarding-profile`의 사용자 프로필 응답에 아래 필드가 포함됩니다.

```json
{
  "currentExerciseStatus": "none",
  "fitnessGoal": "habit",
  "preferredWorkoutPlace": "home",
  "weeklyWorkoutFrequency": 3,
  "availableWorkoutMinutes": 30
}
```

## 온보딩 완료 기준

`onboardingCompleted=true`가 되려면 아래 필수값이 모두 저장되어야 합니다.

```text
gender
birthDate
heightCm
weightKg
experienceLevel
currentExerciseStatus
fitnessGoal
preferredWorkoutPlace
weeklyWorkoutFrequency
availableWorkoutMinutes
```

`preferredExerciseTypes`는 선택값이므로 비어 있어도 온보딩 완료로 처리됩니다.

## 프론트 권장 화면 문구

운동 경험 수준:

```text
처음이에요 -> beginner
조금 해봤어요 -> intermediate
꾸준히 하고 있어요 -> advanced
```

현재 운동 상태:

```text
없어요 -> none
가끔 해요 -> occasional
정기적으로 해요 -> regular
```

운동 목표:

```text
체력 향상 -> stamina
다이어트 -> weight_loss
근력 증가 -> strength
건강/습관 만들기 -> habit
스트레스 해소 -> stress_relief
```

운동 장소:

```text
집 -> home
헬스장 -> gym
야외 -> outdoor
수영장/기타 시설 -> facility
기타 -> other
```

주당 운동 가능 횟수와 1회 운동 가능 시간은 숫자로 보내면 됩니다.
