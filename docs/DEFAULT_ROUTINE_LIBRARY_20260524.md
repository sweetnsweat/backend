# Default Routine Library Seed - 2026-05-24

## 목적

기본 추천 루틴 풀이 기존 초급 중심으로 좁아서, 온보딩 추천 점수에 걸리는 난이도, 목표, 장소, 운동 유형을 다양화했다.

`db/seed/20260524_default_routine_library_seed.sql`은 반복 실행 가능하게 작성했다. 같은 이름의 기본 루틴이 있으면 메타데이터와 세션을 갱신하고, 없으면 새로 추가한다.

## 반영 내용

- 초급 6개 추가
  - 맨몸 근력, 걷기-조깅, 수영, 실내 사이클, 요가/필라테스, 유산소 머신
- 중급 6개 추가
  - 상하체 근력 분할, 러닝 지구력, 수영 컨디셔닝, 사이클 인터벌, 요가/필라테스 밸런스, 하이브리드 감량
- 고급 6개 추가
  - 근력 파워, 러닝 인터벌, 수영 고강도, 사이클 고강도, 하이브리드 컨디셔닝, 파워 요가/필라테스

개발 DB 반영 후 기본 루틴은 총 23개다.

| difficulty | count |
| --- | ---: |
| easy | 11 |
| medium | 6 |
| hard | 6 |

## 추천 로직 연결 필드

각 루틴은 추천 점수에 직접 쓰이는 필드를 채운다.

- `difficulty`: `easy`, `medium`, `hard`
- `target_experience_level`: `beginner`, `intermediate`, `advanced`
- `target_current_exercise_statuses`: `none`, `occasional`, `regular`
- `goal_types`: `habit`, `stamina`, `strength`, `weight_loss`, `stress_relief`
- `place_types`: `home`, `gym`, `outdoor`, `facility`, `other`
- `weekly_frequency`
- `recommended_exercise_types`: 모바일 온보딩 값과 맞춘 `bodyweight`, `strength`, `cardio`, `walking`, `running`, `swimming`, `stretching`, `yoga_pilates`

사이클은 현재 온보딩 운동 유형에 별도 `cycling` 값이 없어서 `recommended_exercise_types`에는 `cardio`로 매핑하고, 실제 운동 아이템은 `exercises.category = '사이클'` 운동으로 연결했다.

## 설계 근거

- CDC 성인 신체활동 가이드라인: 성인은 주 150분 중강도 유산소 또는 동등 강도 활동과 주 2일 이상의 근력 운동을 권장한다.
  - https://www.cdc.gov/physical-activity-basics/guidelines/adults.html
- ACSM Position Stands: 운동처방 및 저항운동 진행 모델을 공식 입장문으로 제공한다.
  - https://acsm.org/education-resources/pronouncements-scientific-communications/position-stands/
- ACSM 저항운동 진행 모델 요약: 초급/중급 저항운동은 주 2-3일, 고급은 주 4-5일 빈도를 권장한다.
  - https://pubmed.ncbi.nlm.nih.gov/11828249/

이 기준을 그대로 의료 처방처럼 강제하지는 않고, 앱의 기본 추천 루틴 seed에서 난이도별 빈도와 강도 구간을 나누는 근거로 사용했다.

## 적용

```bash
ssh dy@100.89.171.113 "docker exec -i postgres-db psql -U postgres -d postgres -v ON_ERROR_STOP=1" \
  < db/seed/20260524_default_routine_library_seed.sql
```
