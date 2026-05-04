# 2026-05-04 프론트 전달 사항

오늘 변경된 프론트 연동 영향은 운동 목록 무한스크롤, 운동 즐겨찾기 조회, 오늘의 루틴 조회, 주간 통계 조회, 루틴 수정/삭제, 루틴 단위 퀘스트, 테스트 데이터, 액세스 토큰 만료 시간이다.

## 1. 운동 목록 무한스크롤 기준

운동 목록과 즐겨찾기 운동 목록은 기본 `size=30`으로 페이지 응답을 내려준다. 모바일 무한스크롤에서는 첫 호출 후 `hasNext=true`일 때 `nextPage`로 다음 페이지를 호출하면 된다.

```http
GET /api/exercises?page=0&size=30
Authorization: Bearer {accessToken}
```

페이지 관련 응답 필드:

```json
{
  "page": 0,
  "size": 30,
  "totalCount": 42,
  "totalPages": 2,
  "first": true,
  "last": false,
  "hasNext": true,
  "nextPage": 1
}
```

필터나 검색어가 바뀌면 `page=0`부터 다시 호출한다.

## 2. 내 즐겨찾기 운동 목록 조회 API 추가

즐겨찾기 탭에서 사용할 명시적 API를 추가했다.

```http
GET /api/users/me/exercises/favorites
Authorization: Bearer {accessToken}
```

쿼리:

| 이름 | 필수 | 예시 | 설명 |
| --- | --- | --- | --- |
| `category` | X | `유산소` | 종목 필터. 전체면 생략하거나 `all` |
| `level` | X | `초급` | 강도/난이도 필터. 전체면 생략하거나 `all` |
| `keyword` | X | `자전거` | 운동 이름 검색 |
| `page` | X | `0` | 기본값 0 |
| `size` | X | `30` | 기본값 30, 최대 100 |

예시:

```http
GET /api/users/me/exercises/favorites?category=유산소
Authorization: Bearer {accessToken}
```

응답은 기존 운동 목록과 같은 `ExerciseListResponse`다. `scope`는 항상 `favorite`로 내려간다.

```json
{
  "success": true,
  "code": "OK",
  "message": "Request succeeded",
  "timestamp": "2026-05-04T09:50:00+09:00",
  "data": {
    "scope": "favorite",
    "category": "유산소",
    "level": null,
    "keyword": null,
    "page": 0,
    "size": 30,
    "totalCount": 1,
    "totalPages": 1,
    "first": true,
    "last": true,
    "hasNext": false,
    "nextPage": null,
    "groups": [
      {
        "category": "유산소",
        "categoryDisplayName": "유산소",
        "count": 1,
        "exercises": [
          {
            "id": 901,
            "name": "실내 자전거",
            "category": "유산소",
            "categoryDisplayName": "유산소",
            "level": "초급",
            "levelDisplayName": "초급",
            "equipment": "실내 자전거",
            "met": 4.0,
            "estimatedKcalPerHour": 280,
            "primaryMuscles": ["전신"],
            "emoji": "🔥",
            "imageUrl": null,
            "liked": true
          }
        ]
      }
    ]
  }
}
```

기존 `GET /api/exercises?scope=favorite`도 같은 결과를 만들 수 있지만, 즐겨찾기 탭에서는 새 API 사용을 권장한다.

## 3. 오늘의 루틴 조회 API 추가

메인 홈의 “오늘의 루틴” 영역은 오늘 퀘스트가 아니라 실제 활성 루틴의 오늘 요일 세션을 보여준다.

```http
GET /api/routines/today
Authorization: Bearer {accessToken}
```

응답 분기:

```text
activeRoutineExists=false -> 활성 루틴 없음. 루틴 설정 CTA 표시
activeRoutineExists=true, routineScheduledToday=false -> 활성 루틴은 있지만 오늘은 쉬는 날
activeRoutineExists=true, routineScheduledToday=true -> session/items로 오늘 루틴 카드 표시
```

오늘 세션이 있는 경우:

```json
{
  "success": true,
  "code": "OK",
  "message": "Request succeeded",
  "timestamp": "2026-05-04T10:10:00+09:00",
  "data": {
    "date": "2026-05-04",
    "dayOfWeek": "MONDAY",
    "dayOfWeekDisplayName": "월요일",
    "activeRoutineExists": true,
    "routineScheduledToday": true,
    "routine": {
      "id": 20,
      "name": "초급 헬스장 근력 입문 루틴",
      "estimatedMinutes": 30,
      "active": true
    },
    "session": {
      "id": 35,
      "sessionName": "상체 머신",
      "sessionType": "upper_body",
      "sessionTypeDisplayName": "상체",
      "estimatedMinutes": 30,
      "items": [
        {
          "id": 91,
          "seq": 1,
          "reps": 10,
          "sets": 3,
          "durationSec": null,
          "restSec": 60,
          "exercise": {
            "id": 10,
            "name": "Chest Press",
            "category": "strength"
          }
        }
      ]
    }
  }
}
```

이 API는 컨디션 입력 여부와 무관하다. 컨디션은 스토리/퀘스트 진입 시 `GET /api/quests/today` 흐름에서만 필요하다.

## 4. 이번 주 통계 조회 API 추가

메인 홈의 “이번 주 통계” 영역에서 사용할 API다.

```http
GET /api/users/me/weekly-stats
Authorization: Bearer {accessToken}
```

계산 기준:

```text
기간 -> KST 기준 이번 주 월요일~일요일
completedWorkoutCount -> 이번 주 COMPLETED 퀘스트 수
maxStreakDays -> 이번 주 안에서 연속 완료한 최대 일수
estimatedCaloriesKcal -> 완료 퀘스트 기반 예상 소모 칼로리
earnedExp -> 이번 주 완료 퀘스트 rewardExp 합계
```

예상 칼로리 기준:

```text
ROUTINE -> 루틴 세션 estimatedMinutes, 세션 운동 MET, 사용자 체중 기준 추정
OFF_DAY -> targetValue 분 * MET 3.0 * 사용자 체중
RECOVERY -> targetValue 분 * MET 2.3 * 사용자 체중
체중이 없으면 성별 fallback, 성별도 없으면 70kg
```

응답:

```json
{
  "success": true,
  "code": "OK",
  "message": "Request succeeded",
  "timestamp": "2026-05-04T10:15:00+09:00",
  "data": {
    "weekStartDate": "2026-05-04",
    "weekEndDate": "2026-05-10",
    "completedWorkoutCount": 7,
    "maxStreakDays": 7,
    "estimatedCaloriesKcal": 8340,
    "earnedExp": 320
  }
}
```

프론트 화면 매핑:

```text
총 운동 -> completedWorkoutCount
연속 달성 -> maxStreakDays
소모 칼로리 -> estimatedCaloriesKcal
이번 주 EXP -> earnedExp
```

주간 랭킹 기준:

```text
이번 주 획득 EXP 합계(earnedExp) 기준으로 내림차순 정렬한다.
동점자는 completedWorkoutCount, estimatedCaloriesKcal 순으로 보조 정렬한다.
칼로리는 추정값이므로 1차 랭킹 기준으로 사용하지 않는다.
```

## 5. 루틴 있는 날 퀘스트가 루틴 단위로 변경

기존에는 오늘 세션 운동 중 일부 운동 개수를 목표로 내려갔다. 이제는 오늘 세션 전체 루틴 1회를 완료하는 퀘스트로 내려간다.

변경 전:

```json
{
  "questType": "ROUTINE",
  "targetMetric": "EXERCISES",
  "title": "상체 머신 운동 2개 완료",
  "targetValue": 2
}
```

변경 후:

```json
{
  "questType": "ROUTINE",
  "targetMetric": "ROUTINE",
  "title": "상체 머신 루틴 완료",
  "description": "상체 머신 세션의 운동 루틴을 완료해 주세요. 포함된 운동은 총 3개입니다.",
  "targetValue": 1,
  "conditionAdjusted": false,
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

프론트 처리 기준:

- `targetMetric=ROUTINE`이면 오늘 세션 전체 완료 퀘스트로 표시한다.
- `targetValue=1`은 루틴 1회 완료를 의미한다.
- `exercises`에는 오늘 세션에 포함된 전체 운동 스냅샷이 내려간다.
- `conditionAdjusted`는 루틴 단위 퀘스트에서는 기본 `false`다.
- 컨디션이 매우 낮으면 기존처럼 `RECOVERY` 퀘스트가 내려갈 수 있다.
- 오늘 요일에 루틴 세션이 없으면 기존처럼 `OFF_DAY` 퀘스트가 내려간다.

## 6. 루틴 수정/삭제 API 추가

직접 만든 루틴 화면에서 편집/삭제를 붙일 수 있도록 API를 추가했다.

```http
PUT /api/routines/{routineId}
DELETE /api/routines/{routineId}
Authorization: Bearer {accessToken}
```

수정 API는 `POST /api/routines/custom`과 거의 같은 요청 구조를 사용한다. `activate`는 수정 요청에 포함하지 않는다.

```json
{
  "name": "수정 후 루틴",
  "description": "수정된 설명",
  "sessions": [
    {
      "dayOfWeek": "WEDNESDAY",
      "sessionName": "수요일 전신",
      "sessionType": "full_body",
      "estimatedMinutes": 35,
      "items": [
        {
          "exerciseId": 109,
          "sets": 3,
          "reps": 12,
          "restSec": 60
        }
      ]
    }
  ]
}
```

수정은 세션/운동 항목을 부분 수정하지 않고 요청 내용으로 전체 교체한다. 응답은 수정된 `RoutineDetailResponse`다.

`items[].seq`는 루틴 전체 순서가 아니라 세션 내부 순서다. 월요일 세션에서 `1,2`, 수요일 세션에서 다시 `1`, 금요일 세션에서 다시 `1`처럼 보내도 정상 처리된다. `seq`를 생략하면 해당 세션 배열 순서대로 1부터 부여된다.

같은 세션 안에서만 `seq` 중복이 금지된다. 이 경우 `400 DUPLICATE_ROUTINE_ITEM_SEQ`가 내려간다.

DB 제약도 루틴 전체 기준이 아니라 세션 기준으로 변경됐다.

```text
변경 전: routine_items(routine_id, seq) unique
변경 후: routine_items(routine_session_id, seq) unique
```

적용 SQL:

```text
db/20260504_routine_item_seq_per_session.sql
```

삭제는 물리 삭제가 아니라 비활성 처리다. 삭제한 루틴이 현재 활성 루틴이면 `GET /api/users/me`의 `activeRoutineId`가 `null`이 된다.

프론트 처리 기준:

- 기본 루틴 원본은 수정/삭제하지 않는다.
- `GET /api/users/me/routines`에 내려오는 사용자 루틴만 수정/삭제 버튼을 노출한다.
- 삭제 성공 후 내 루틴 목록과 `GET /api/users/me`를 다시 조회한다.
- 삭제한 루틴이 활성 루틴이었다면 오늘의 루틴은 `activeRoutineExists=false`로 분기된다.

## 7. 루틴 목록/활성 루틴 API 구분

아래 API는 의미가 다르다. 여러 개가 필요하면 반드시 목록 API를 호출해야 한다.

```http
GET /api/users/me/routines
```

현재 로그인 사용자의 사용자 루틴 목록을 내려준다. 직접 만든 루틴과 추천 루틴을 선택해서 복사된 루틴이 포함된다.

```http
GET /api/users/me/routines/active
```

현재 활성화된 루틴 1개 상세만 내려준다. DB 기준은 `users.active_routine_id`다.

판정 기준:

```text
users.active_routine_id is null -> 활성 루틴 없음
users.active_routine_id = routines.id and routines.is_active = true -> 활성 루틴 있음
```

`routines.is_active`는 삭제/비활성 여부이고, 사용자가 선택한 활성 루틴 여부는 `users.active_routine_id`로 판단한다.

## 8. 액세스 토큰 만료 시간 변경

액세스 토큰 기본 만료 시간을 15분에서 1시간으로 늘렸다.

```text
변경 전: 900초
변경 후: 3600초
```

적용 위치:

```text
src/main/resources/application.properties
src/test/resources/application.properties
docker-compose.yml
```

이미 발급된 토큰은 기존 만료시간을 따른다. 새로 로그인해서 받은 토큰부터 1시간이 적용된다.

운영/개발 서버에서 `JWT_ACCESS_TOKEN_SECONDS` 환경변수를 별도로 지정하고 있으면 그 값이 우선한다.

## 9. 로컬 테스트 계정/더미 데이터

로컬 DB에 테스트용 계정과 루틴/퀘스트/즐겨찾기 데이터를 넣어뒀다. 비밀번호는 모두 같다.

```text
password: password123
```

주요 계정:

| loginId | 용도 |
| --- | --- |
| `demo_routine_editor` | 루틴 목록/상세/수정/삭제 테스트 |
| `demo_stats_suyeon` | 주간 통계, 마이페이지, 랭킹 테스트 |
| `demo_gym_quest` | 활성 루틴 기반 오늘 퀘스트 생성 테스트 |
| `demo_recovery_quest` | 낮은 컨디션 회복 퀘스트 생성 테스트 |
| `demo_recommend` | 온보딩 기반 추천 루틴 테스트 |

`demo_routine_editor`의 현재 DB 상태:

```text
보유 루틴:
- 데모 수정 가능 루틴              active=true
- 데모 추가 루틴 - 주2회 밸런스    active=false
```

루틴 ID는 로컬 DB와 개발 서버 DB의 자동 증가 상태에 따라 다를 수 있으므로 API 응답의 `id`를 사용한다.

루틴 목록 여러 개를 확인하려면:

```http
GET /api/users/me/routines
Authorization: Bearer {accessToken}
```

활성 루틴 1개만 확인하려면:

```http
GET /api/users/me/routines/active
Authorization: Bearer {accessToken}
```

## 10. 관련 문서

- `EXERCISE_API_FRONTEND_GUIDE.md`: 즐겨찾기 조회 API 상세
- `ROUTINE_API_FRONTEND_GUIDE.md`: 오늘의 루틴 조회, 루틴 수정/삭제 API 상세
- `QUEST_API_FRONTEND_GUIDE.md`: 퀘스트 생성/응답 상세
- `FRONTEND_DEVELOPMENT_SUMMARY_20260501.md`: 종합 흐름 최신화
