# 2026-05-04 프론트 전달 사항

오늘 변경된 프론트 연동 영향은 홈 상단 세계관 슬라이드, 세계관 랭킹, 이번 주 활동 랭킹, 레벨/EXP/Gold 보상 정책, 운동 카테고리/운동 목록 무한스크롤, 운동 즐겨찾기 조회, 오늘의 루틴 조회, 주간 통계 조회, 루틴 수정/삭제, 루틴 단위 퀘스트, 테스트 데이터, 액세스 토큰 만료 시간이다.

## 0. 홈 상단 세계관 슬라이드 API

메인 홈 상단 캐러셀 카드에 표시할 활성 세계관과 대표 캐릭터 정보를 조회한다.

```http
GET /api/home/world-banners
Authorization: Bearer {accessToken}
```

쿼리:

| 이름 | 필수 | 기본값 | 설명 |
| --- | --- | --- | --- |
| `limit` | X | `3` | 조회할 슬라이드 개수. 1~20 |

응답:

```json
{
  "success": true,
  "code": "OK",
  "message": "Request succeeded",
  "data": {
    "slides": [
      {
        "scenarioId": 4,
        "worldTitle": "월하검귀는 다시 웃지 않는다",
        "genre": "무협 회귀 복수 로맨스",
        "summary": "복수와 회귀를 다루는 무협 세계관",
        "imageUrl": "/media/assets/character_천류하_8b1e61b5eb.png",
        "backgroundImageUrl": "/media/assets/world_월하검귀는-다시-웃지-않는다_9c98d72cc8.png",
        "representativeCharacterName": "천류하",
        "representativeCharacterTitle": "검귀",
        "headline": "천류하",
        "quote": "네 뒤를 쫓아가도, 결코 네 그림자를 벗어날 수 없다는 걸 알게 될 거야."
      }
    ]
  }
}
```

조회 기준:

- `scenarios.is_active=true`인 세계관만 내려간다.
- 기본 정렬은 `scenarioId desc`다.
- 대표 캐릭터는 `character_profiles.is_representative=true`인 캐릭터를 우선 사용한다.
- `imageUrl`은 대표 캐릭터 이미지가 있으면 우선 사용하고, 없으면 세계관 썸네일/세계관 이미지로 대체한다.
- `backgroundImageUrl`은 세계관 이미지가 있으면 우선 사용하고, 없으면 썸네일/대표 캐릭터 이미지로 대체한다.

## 세계관 랭킹 API

메인 홈의 `세계관 랭킹` 섹션에 표시할 세계관 순위를 조회한다.

```http
GET /api/worlds/rankings
Authorization: Bearer {accessToken}
```

쿼리:

| 이름 | 필수 | 기본값 | 설명 |
| --- | --- | --- | --- |
| `limit` | X | `5` | 조회할 랭킹 개수. 1~20 |

랭킹 기준:

- `story_progress.status='IN_PROGRESS'`인 진행 상태만 집계한다.
- `score`는 해당 세계관의 진행 중인 사용자 수다.
- `story_play_logs`는 메시지 단위 로그라서 랭킹에는 사용하지 않는다.
- `activeChatCount` 필드는 따로 내려주지 않고 `score`만 사용한다.

응답:

```json
{
  "success": true,
  "code": "OK",
  "message": "Request succeeded",
  "data": {
    "metric": "ACTIVE_CHAT_COUNT",
    "rankings": [
      {
        "rank": 1,
        "scenarioId": 4,
        "worldTitle": "월하검귀는 다시 웃지 않는다",
        "displayName": "천류하",
        "imageUrl": "/media/assets/character_천류하_8b1e61b5eb.png",
        "score": 3
      }
    ]
  }
}
```

## 이번 주 활동 랭킹 API

메인 홈의 `이번 주 랭킹` 섹션에 표시할 사용자 활동 순위를 조회한다.

```http
GET /api/rankings/weekly-activity
Authorization: Bearer {accessToken}
```

쿼리:

| 이름 | 필수 | 기본값 | 설명 |
| --- | --- | --- | --- |
| `size` | X | `3` | 조회할 사용자 수. 1~100 |

랭킹 기준:

- KST 기준 이번 주 월요일부터 일요일까지 집계한다.
- `user_quests.status='completed'`인 퀘스트만 집계한다.
- `score`가 아니라 `weeklyExp`로 내려간다.
- `weeklyExp`는 해당 기간 `user_quests.reward_exp` 합계다.
- 유저 프로필 이미지는 내려주지 않는다. 필요하면 프론트에서 기본 이미지를 사용한다.

응답:

```json
{
  "success": true,
  "code": "OK",
  "message": "Request succeeded",
  "data": {
    "weekStartDate": "2026-05-04",
    "weekEndDate": "2026-05-10",
    "metric": "WEEKLY_EXP",
    "rankings": [
      {
        "rank": 1,
        "userId": 15,
        "nickname": "하준",
        "weeklyExp": 1240,
        "isMe": false
      },
      {
        "rank": 2,
        "userId": 16,
        "nickname": "수연",
        "weeklyExp": 980,
        "isMe": true
      }
    ]
  }
}
```

## 레벨, EXP, Gold 보상 정책

퀘스트 완료 시 EXP와 Gold가 지급된다. 보상 기준은 `운동 시간 기반 노력량`이다.

정책 근거:

- 루틴 세션에는 이미 `estimatedMinutes`가 있어서 백엔드와 프론트가 같은 기준을 공유할 수 있다.
- MET/칼로리는 운동 종류, 체중, 수행 강도에 따라 편차가 커서 보상 기준으로 쓰면 특정 운동만 선택하는 파밍이 생길 수 있다.
- 초보자 루틴도 완료 경험을 보상해야 하므로 최소 보상을 둔다.
- 너무 긴 루틴으로 보상이 과하게 튀지 않도록 최대 보상을 둔다.

현재 지급 기준:

| 퀘스트 유형 | EXP | Gold |
| --- | --- | --- |
| 루틴 퀘스트 | 세션 예상 시간을 5 단위 반올림, 최소 20, 최대 60 | EXP의 절반을 5 단위 반올림, 최소 5 |
| 오프데이 퀘스트 | 목표 시간을 5 단위 반올림, 최소 10, 최대 25 | EXP의 절반을 5 단위 반올림, 최소 5 |
| 회복 퀘스트 | 10 | 5 |

레벨은 누적 EXP 기준이다.

```text
레벨 N 도달 누적 EXP = 50 * N * (N - 1)
```

예시:

| 레벨 | 필요 누적 EXP |
| --- | ---: |
| 1 | 0 |
| 2 | 100 |
| 3 | 300 |
| 4 | 600 |
| 5 | 1,000 |
| 10 | 4,500 |

보상은 한 퀘스트당 한 번만 지급된다. 같은 퀘스트 완료 API를 여러 번 호출해도 EXP와 Gold가 중복 증가하지 않는다.

`GET /api/quests/today`, `GET /api/quests/today/by-user`, `PATCH /api/quests/{questId}/complete`의 `QuestResponse`에는 퀘스트 보상도 함께 내려간다.

```json
{
  "rewardExp": 30,
  "rewardCurrency": 15,
  "rewardGold": 15
}
```

`rewardCurrency`는 기존 호환 필드이고, 프론트에서는 이름이 명확한 `rewardGold`를 사용하면 된다.

`GET /api/users/me`, 로그인 응답, 온보딩 저장 응답에 아래 필드가 추가된다.

```json
{
  "level": 3,
  "totalExp": 420,
  "currentLevelExp": 120,
  "nextLevelRequiredExp": 300,
  "nextLevelRemainingExp": 180,
  "balanceCurrency": 65
}
```

상세 정책은 `docs/REWARD_POLICY_GUIDE.md`를 기준으로 보면 된다.

## AI 스토리 Swagger 노출 정리

백엔드 Swagger에서 프론트가 직접 볼 AI 프록시 API는 아래 4개만 노출한다.

```http
GET /api/ai/health
POST /api/stories/generate
POST /api/stories/play
GET /api/stories/play/history
```

아래 legacy 세부 플레이 API는 기존 호환을 위해 서버에는 남아 있지만 Swagger에서는 숨긴다. 신규 프론트는 사용하지 않는다.

```http
POST /api/stories/play/start
POST /api/stories/play/continue
POST /api/stories/play/choose
POST /api/stories/play/next-chapter
```

`POST /api/stories/generate`는 더 이상 Swagger에서 단순 문자열로 보이지 않는다. AI 서버의 `StoryTemplateInput`과 같은 의미의 DTO로 표시된다.

필수 필드:

```text
genre
world_setting
tone_and_mood
player_role
core_conflict
characters[].name
characters[].role
characters[].personality
```

주요 선택 필드:

```text
title
thumbnail_url
generate_images
trope_style
drama_intensity
pacing_style
required_events
forbidden_elements
ending_direction
chapter_count
characters[].relationship_to_player
characters[].background
characters[].special_notes
```

예시:

```json
{
  "title": "붉은 달의 계약",
  "thumbnail_url": "https://example.com/thumbnails/red-moon.jpg",
  "generate_images": false,
  "genre": "로맨스 판타지",
  "trope_style": "회귀/빙의/환생",
  "drama_intensity": "high",
  "pacing_style": "shortform_melodrama",
  "world_setting": "마법과 귀족 정치가 공존하는 제국. 붉은 달이 뜨는 밤마다 금지된 계약이 깨어난다.",
  "tone_and_mood": "긴장감 있고 서정적이며, 인물 간 감정선이 중요하다.",
  "player_role": "기억을 잃고 제국 신전에 나타난 계약자",
  "core_conflict": "플레이어는 자신의 정체와 제국을 뒤흔들 예언 사이에서 선택해야 한다.",
  "required_events": "황태자와의 첫 만남, 금지된 마법의 발현, 배신자의 등장",
  "forbidden_elements": "지나치게 코믹한 전개",
  "ending_direction": "플레이어가 스스로 운명을 선택하는 결말",
  "chapter_count": 5,
  "characters": [
    {
      "name": "리안",
      "role": "황태자",
      "personality": "차갑고 신중하지만 플레이어에게만 약한 면을 보인다.",
      "relationship_to_player": "처음에는 경계하지만 점점 신뢰하게 되는 인물",
      "background": "제국의 정치적 음모 속에서 살아남은 후계자",
      "special_notes": "플레이어가 위험해질 때 감정이 크게 흔들린다."
    }
  ]
}
```

`GET /api/stories/play/history`는 AI 서버의 대화 히스토리 조회 API를 백엔드가 감싼 것이다. AI 서버 원본은 `user_id`, `scenario_id`, `limit`, `offset`을 받지만, 백엔드는 `user_id`를 JWT 로그인 사용자 ID로 주입한다. 프론트는 `user_id`를 보내지 않는다.

```http
GET /api/stories/play/history?scenario_id=4&limit=100&offset=0
Authorization: Bearer {accessToken}
```

쿼리:

| 이름 | 필수 | 기본값 | 설명 |
| --- | --- | --- | --- |
| `scenario_id` | O | - | 조회할 시나리오 ID |
| `limit` | X | `100` | 가져올 로그 수. 1~300 |
| `offset` | X | `0` | 건너뛸 로그 수 |

응답의 `data.items`는 AI 서버 응답을 그대로 감싼다.

```json
{
  "items": [
    {
      "role": "assistant",
      "character_name": "리안",
      "content": "여기가 어디인지 정말 모르는 건가?"
    }
  ]
}
```

AI 서버가 스토리 중 운동 퀘스트를 세계관 문장으로 래핑해야 할 때는 아래 백엔드 API를 사용한다. 캡스톤 데모 편의를 위해 이 API는 토큰 없이 `userId`만 받는다.

```http
GET /api/quests/today/by-user?userId=14
```

응답은 기존 `GET /api/quests/today`와 같은 `QuestResponse`다. 상세 내용은 `AI_QUEST_API_GUIDE.md`를 기준으로 본다.

## 1. 운동 카테고리/목록 무한스크롤 기준

운동 카테고리, 운동 목록, 즐겨찾기 운동 목록은 기본 `size=30`으로 페이지 응답을 내려준다. 모바일 무한스크롤에서는 첫 호출 후 `hasNext=true`일 때 `nextPage`로 다음 페이지를 호출하면 된다.

```http
GET /api/exercises/categories?page=0&size=30
Authorization: Bearer {accessToken}

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

직접 루틴 생성/수정 요청에서는 `sessionType` 코드값만 보낸다. 응답의 `sessions[]`에는 화면 표시용 한글 라벨 `sessionTypeDisplayName`이 함께 내려간다.

```json
{
  "sessionType": "full_body",
  "sessionTypeDisplayName": "전신"
}
```

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
