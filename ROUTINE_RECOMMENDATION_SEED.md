# 루틴 추천 Seed 설계

온보딩에서 받은 값을 기반으로 기본 루틴을 1~2개 추천하기 위해 `routines` 테이블에 추천 메타데이터를 추가했다. 추천은 초기 MVP에서 rule-based scoring으로 구현할 수 있도록 설계했다.

## 추가한 루틴 메타데이터

| 컬럼 | 타입 | 온보딩 필드 | 넣은 이유 |
| --- | --- | --- | --- |
| `target_experience_level` | `varchar(20)` | `experienceLevel` | 초보자에게 고강도 루틴이 추천되지 않도록 루틴의 대상 경험 수준을 명시한다. |
| `target_current_exercise_statuses` | `jsonb` | `currentExerciseStatus` | 같은 초보자라도 현재 운동을 전혀 안 하는 사람과 가끔 하는 사람은 추천 강도가 다르므로, 루틴이 커버하는 현재 운동 상태를 배열로 둔다. |
| `goal_types` | `jsonb` | `fitnessGoal` | 하나의 루틴이 `habit`, `stamina`처럼 여러 목표에 맞을 수 있어 배열로 둔다. |
| `place_types` | `jsonb` | `preferredWorkoutPlace` | 스트레칭처럼 집/헬스장/시설에서 모두 가능한 루틴이 있어 배열로 둔다. |
| `weekly_frequency` | `integer` | `weeklyWorkoutFrequency` | 사용자의 주당 가능 횟수와 루틴이 의도한 주당 반복 횟수 차이를 점수화하기 위해 둔다. |
| `recommended_exercise_types` | `jsonb` | `preferredExerciseTypes` | 선택 입력인 선호 운동 유형과 겹치면 추천 점수를 보정하기 위해 둔다. |

`availableWorkoutMinutes`는 기존 `estimated_minutes`와 비교하면 되므로 새 컬럼을 만들지 않았다.

## Seed 루틴

현재 기본 루틴은 아래 5개다.

| 루틴 | 주요 대상 |
| --- | --- |
| `초급 전신 루틴` | 운동을 처음 시작하고 집에서 짧게 전신/맨몸 운동을 하려는 사용자 |
| `회복 스트레칭 루틴` | 스트레스 해소, 습관 만들기, 회복 중심의 낮은 강도 루틴이 필요한 사용자 |
| `유산소 스타터 루틴` | 헬스장에서 걷기/러닝머신 기반으로 체력/다이어트를 시작하려는 사용자 |
| `초급 헬스장 근력 입문 루틴` | 헬스장에서 머신과 가벼운 유산소로 근력 운동을 시작하려는 사용자 |
| `초급 야외 걷기 루틴` | 야외에서 걷기와 가벼운 조깅으로 운동 습관을 만들려는 사용자 |

## 추천 점수 예시

추천 API는 다음 방식으로 단순 점수화하면 된다.

```text
경험 수준 일치: +30
현재 운동 상태 포함: +15
목표 포함: +25
장소 포함: +20
루틴 예상 시간이 사용자 가능 시간 이하: +10
주당 횟수 차이가 1 이하: +10
선호 운동 유형 겹침: +5 per match
```

이후 점수 상위 1~2개를 내려주면 된다.

## 적용 SQL

`db/seed/20260501_routine_recommendation_metadata_seed.sql`

이 SQL은 컬럼 추가와 seed 보강을 함께 수행하며, 같은 루틴이 이미 있으면 중복 생성하지 않는다.
