# 2026-05-24 프론트 연동 가이드

이 문서는 2026-05-24 기준 배틀 매칭 구조 변경사항을 정리한다.

- 배틀 매칭은 이제 active 유저 자동 선택이 아니라 `battle_match_queue` 기반 대기열 매칭이다.
- 배틀에 참가하지 않은 유저는 상대방으로 잡히지 않는다.
- 기존 `POST /api/battles/match` 엔드포인트는 유지한다.
- 프론트는 응답의 `data.matchStatus`로 `WAITING` / `MATCHED`를 분기하면 된다.
- 상점 패스 아이템은 `POST /api/shop/items/{itemId}/use`로 실제 서버 효과를 활성화/소비한다.
- 획득 배지는 퀘스트/배틀 기록 기준으로 백엔드가 자동 지급한다.

---

## 1. 배틀 매칭 흐름

### 변경 전

```text
사용자 A가 배틀 참가
-> active 유저 풀에서 상대 자동 선택
-> 상대가 배틀 참가를 누르지 않았어도 배틀 생성 가능
```

### 변경 후

```text
사용자 A가 배틀 참가
-> battle_match_queue에 WAITING 등록
-> 아직 상대가 없으면 WAITING 응답

사용자 B가 배틀 참가
-> 같은 모드/기간의 WAITING 사용자 A 조회
-> A-B 배틀 생성
-> 두 큐 row를 MATCHED 처리
-> MATCHED 응답
```

즉, 이제 실제로 배틀 참가 API를 호출한 사용자끼리만 매칭된다.

---

## 2. 배틀 매칭 요청

```http
POST /api/battles/match
Authorization: Bearer {accessToken}
Content-Type: application/json
```

요청:

```json
{
  "mode": "DAILY"
}
```

`mode`:

| value | meaning |
| --- | --- |
| `DAILY` | 오늘 하루 배틀 |
| `WEEKLY` | 이번 주 배틀 |

---

## 3. 대기 상태 응답

같은 모드/기간에 대기 중인 상대가 없으면 배틀을 바로 만들지 않고 대기열에 등록한다.

응답:

```json
{
  "success": true,
  "code": "OK",
  "message": "Battle queued",
  "data": {
    "battleId": null,
    "mode": "DAILY",
    "status": null,
    "periodStartDate": "2026-05-24",
    "periodEndDate": "2026-05-24",
    "startsAt": "2026-05-23T15:00:00Z",
    "endsAt": "2026-05-24T15:00:00Z",
    "remainingSeconds": 27441,
    "matchStatus": "WAITING",
    "queuedAt": "2026-05-24T07:22:38.656985660Z",
    "participants": [
      {
        "userId": 87,
        "nickname": "userA",
        "profileImageUrl": null,
        "me": true,
        "score": 0,
        "result": "PENDING"
      }
    ],
    "score": {
      "myScore": 0,
      "opponentScore": null,
      "leadingUserId": null
    },
    "metrics": []
  }
}
```

프론트 처리:

- `data.matchStatus === "WAITING"`이면 "상대 찾는 중" 화면을 보여준다.
- `data.battleId`는 `null`이므로 배틀 상세 화면으로 이동하면 안 된다.
- 같은 사용자가 다시 호출해도 기존 WAITING 큐를 재사용한다.
- 폴링을 할 경우 같은 `POST /api/battles/match`를 다시 호출하면 된다.

---

## 4. 매칭 완료 응답

대기 중인 상대가 있으면 배틀이 생성된다.

응답:

```json
{
  "success": true,
  "code": "OK",
  "message": "Battle matched",
  "data": {
    "battleId": 5,
    "mode": "DAILY",
    "status": "ACTIVE",
    "periodStartDate": "2026-05-24",
    "periodEndDate": "2026-05-24",
    "startsAt": "2026-05-23T15:00:00Z",
    "endsAt": "2026-05-24T15:00:00Z",
    "remainingSeconds": 27441,
    "matchStatus": "MATCHED",
    "queuedAt": null,
    "participants": [
      {
        "userId": 88,
        "nickname": "userB",
        "profileImageUrl": null,
        "me": true,
        "score": 0,
        "result": "PENDING"
      },
      {
        "userId": 87,
        "nickname": "userA",
        "profileImageUrl": null,
        "me": false,
        "score": 0,
        "result": "PENDING"
      }
    ],
    "score": {
      "myScore": 0,
      "opponentScore": 0,
      "leadingUserId": null
    }
  }
}
```

프론트 처리:

- `data.matchStatus === "MATCHED"`이면 `data.battleId`로 배틀 상세 화면에 진입한다.
- 이미 같은 기간/모드의 ACTIVE 배틀이 있는 사용자가 다시 호출해도 기존 배틀 상세가 `MATCHED`로 반환된다.
- 기존에 이미 만들어진 ACTIVE 배틀은 큐 로직과 무관하게 계속 유지된다.

---

## 5. 화면 분기 예시

```ts
type BattleMatchStatus = 'WAITING' | 'MATCHED';

type BattleMatchResponse = {
  battleId: number | null;
  mode: 'DAILY' | 'WEEKLY';
  status: 'ACTIVE' | 'FINALIZED' | 'CANCELLED' | null;
  matchStatus: BattleMatchStatus;
  queuedAt: string | null;
  participants: BattleParticipant[];
  score: {
    myScore: number;
    opponentScore: number | null;
    leadingUserId: number | null;
  };
};

if (response.data.matchStatus === 'WAITING') {
  // 상대 찾는 중 화면 유지
}

if (response.data.matchStatus === 'MATCHED' && response.data.battleId) {
  // battleId로 배틀 상세 화면 이동
}
```

---

## 6. 배틀 점수 및 건강 데이터 반영

배틀 지표는 이제 퀘스트 완료 proof에만 의존하지 않는다. 프론트가 `/api/health-data/sync`로 건강 데이터를 동기화하면 백엔드가 일별 요약을 저장하고, 배틀은 해당 기간의 `health_daily_summaries` 값을 우선 사용한다.

```http
POST /api/health-data/sync
Authorization: Bearer {accessToken}
Content-Type: application/json
```

요청 예시:

```json
{
  "samples": [
    {
      "source": "health_connect",
      "rawRecordType": "Steps",
      "value": 3000,
      "unit": "count",
      "startTime": "2026-05-24T00:00:00Z",
      "endTime": "2026-05-24T01:00:00Z",
      "dataOrigin": "com.google.android.apps.fitness"
    },
    {
      "source": "health_connect",
      "rawRecordType": "Distance",
      "value": 2000,
      "unit": "m",
      "startTime": "2026-05-24T00:00:00Z",
      "endTime": "2026-05-24T01:00:00Z",
      "dataOrigin": "com.google.android.apps.fitness"
    },
    {
      "source": "health_connect",
      "rawRecordType": "ActiveCaloriesBurned",
      "value": 150,
      "unit": "kcal",
      "startTime": "2026-05-24T00:00:00Z",
      "endTime": "2026-05-24T01:00:00Z",
      "dataOrigin": "com.google.android.apps.fitness"
    },
    {
      "source": "health_connect",
      "rawRecordType": "ExerciseSession",
      "value": 20,
      "unit": "minutes",
      "startTime": "2026-05-24T00:00:00Z",
      "endTime": "2026-05-24T00:20:00Z",
      "dataOrigin": "com.google.android.apps.fitness"
    }
  ]
}
```

배틀 점수 계산:

```text
완료 퀘스트 수 * 100
+ 검증 완료 퀘스트 수 * 50
+ 운동 시간(분) * 10
+ 이동 거리(m) * 0.03
+ 걸음 수 * 0.01
+ 활동 칼로리(kcal) * 2
```

퀘스트 완료 방식별 처리:

| completionType | 완료 퀘스트 점수 | 건강 데이터 지표 | 비고 |
| --- | --- | --- | --- |
| `VERIFIED` | O | O | health sync 데이터가 있으면 sync 값을 우선 사용 |
| `MANUAL` | O | O | 수동 완료여도 health sync가 있으면 걸음/거리/칼로리/운동시간 반영 |

즉 수동 완료만 하고 건강 데이터 동기화가 없으면 완료 퀘스트 점수만 반영된다. 건강 데이터 동기화가 있으면 수동 완료 여부와 별개로 운동 시간, 걸음 수, 이동 거리, 활동 칼로리도 배틀 지표에 반영된다.

---

## 7. 상점 패스 효과 적용

패스 아이템은 구매만으로 효과가 적용되지 않는다. 프론트가 보유 중인 패스 아이템에 대해 사용 API를 호출해야 서버 효과가 적용된다.

```http
POST /api/shop/items/{itemId}/use
Authorization: Bearer {accessToken}
```

요청 바디는 없다.

### 7.1 공통 응답

```json
{
  "success": true,
  "code": "OK",
  "message": "아이템을 사용했습니다.",
  "data": {
    "item": {
      "id": 427,
      "itemId": 13,
      "itemType": "ticket",
      "name": "EXP 2배권",
      "description": "24시간 동안 경험치가 2배로 쌓여요",
      "quantity": 0,
      "imageUrl": "/media/assets/item_exp_boost.png",
      "metadata": {
        "effect": "EXP x2 · 24시간",
        "special": false
      }
    },
    "effectType": "EXP_BOOST",
    "status": "ACTIVE",
    "message": "24시간 동안 EXP 2배 효과가 적용됩니다.",
    "expiresAt": "2026-05-25T08:30:00Z",
    "quest": null
  }
}
```

`item.quantity`는 사용 후 남은 보유 수량이다.

보유하지 않은 아이템이면:

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "보유한 아이템만 사용할 수 있습니다.",
  "code": "ITEM_NOT_OWNED",
  "path": "/api/shop/items/13/use"
}
```

사용 효과가 없는 아이템이면:

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "사용 효과가 없는 아이템입니다.",
  "code": "ITEM_NOT_USABLE",
  "path": "/api/shop/items/17/use"
}
```

### 7.2 패스별 효과

| 아이템 | effectType | 적용 방식 |
| --- | --- | --- |
| `EXP 2배권` | `EXP_BOOST` | 사용 시 24시간 활성화. 활성 시간 동안 퀘스트 EXP와 배틀 승리 EXP가 2배 지급된다. |
| `퀘스트 스킵권` | `QUEST_SKIP` | 사용 즉시 오늘 퀘스트를 수동 완료 처리한다. 이미 완료된 오늘 퀘스트에는 사용할 수 없다. |
| `기록 방어권` | `RECORD_SHIELD` | 다음 배틀 종료 시 내 최종 점수가 기존 같은 모드 최고 점수보다 낮으면 최고 점수로 방어하고 1회 소비된다. |
| `승률하락 방어권` | `WIN_RATE_SHIELD` | 다음 배틀 패배 시 결과를 `DRAW`로 바꿔 승률 하락을 막고 1회 소비된다. |
| `배틀 부활권` | `BATTLE_RETRY` | 현재 백엔드에서는 다음 배틀 패배 시 `DRAW`로 방어하는 1회성 패배 방어권으로 적용된다. |

### 7.3 퀘스트 스킵권 응답

```json
{
  "success": true,
  "code": "OK",
  "message": "아이템을 사용했습니다.",
  "data": {
    "item": {
      "id": 501,
      "itemId": 6,
      "itemType": "ticket",
      "name": "퀘스트 스킵권",
      "quantity": 0,
      "imageUrl": "/media/assets/item_quest_skip.png",
      "metadata": {
        "effect": "퀘스트 스킵 · 1회",
        "special": false
      }
    },
    "effectType": "QUEST_SKIP",
    "status": "USED",
    "message": "오늘 퀘스트를 스킵권으로 완료했습니다.",
    "expiresAt": null,
    "quest": {
      "id": 12,
      "status": "COMPLETED",
      "completed": true,
      "completionType": "MANUAL",
      "verificationStatus": "NOT_PROVIDED",
      "battleEligible": false,
      "rewardExp": 10,
      "rewardCurrency": 5,
      "rewardGold": 5
    }
  }
}
```

이미 완료된 오늘 퀘스트에 사용하면:

```json
{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "detail": "이미 완료된 오늘 퀘스트에는 스킵권을 사용할 수 없습니다.",
  "code": "QUEST_ALREADY_COMPLETED",
  "path": "/api/shop/items/6/use"
}
```

### 7.4 프론트 처리 권장

- 패스 탭에서 `ownedQuantity > 0`이면 구매 버튼 대신 `사용하기` 버튼을 노출한다.
- `use` 성공 후 `GET /api/shop/items?type=pass`를 다시 조회해 남은 수량을 갱신한다.
- `EXP_BOOST`, `RECORD_SHIELD`, `WIN_RATE_SHIELD`, `BATTLE_RETRY`는 서버가 이후 보상/배틀 확정 시 자동 적용한다.
- `QUEST_SKIP`은 응답의 `data.quest`로 오늘 퀘스트 완료 상태를 즉시 갱신할 수 있다.

---

## 8. 획득 배지 API

획득 배지는 별도 테이블을 만들지 않고 기존 `items` / `user_items` 구조를 사용한다.

- DB 타입은 기존 제약에 맞춰 `items.item_type = pvp_badge`
- 배지 구분은 `metadata.kind = achievement_badge`
- 구매 불가 배지이므로 `is_sellable = false`, `price_currency = 0`
- 프론트는 `GET /api/users/me/badges`를 우선 사용하면 된다.

### 8.1 자동 지급 기준

| badgeCode | 이름 | 지급 기준 |
| --- | --- | --- |
| `FIRST_QUEST_COMPLETE` | 첫 퀘스트 완료 | 퀘스트 1회 완료 |
| `VERIFIED_QUEST_COMPLETE` | 검증 완료 | 건강 데이터로 검증된 퀘스트 1회 완료 |
| `QUEST_STREAK_3` | 3일 연속 달성 | 퀘스트 3일 연속 완료 |
| `QUEST_10_COMPLETE` | 퀘스트 루키 | 퀘스트 누적 10회 완료 |
| `FIRST_BATTLE_JOIN` | 첫 배틀 참가 | 배틀 1회 참가 후 결과 확정 |
| `FIRST_BATTLE_WIN` | 첫 승리 | 배틀 1회 승리 |
| `BATTLE_SCORE_1000` | 1000점 돌파 | 배틀 정산 점수 1000점 이상 |

자동 지급 타이밍:

- 퀘스트 완료 성공 직후 퀘스트 관련 배지를 재평가한다.
- 배틀 결과 확정 직후 배틀 관련 배지를 재평가한다.
- 이미 지급된 배지는 `user_items` unique 제약 기준으로 중복 지급하지 않는다.

### 8.2 내 배지 목록 조회

```http
GET /api/users/me/badges
Authorization: Bearer {accessToken}
```

응답:

```json
{
  "success": true,
  "code": "OK",
  "message": "OK",
  "data": {
    "badges": [
      {
        "itemId": 31,
        "badgeCode": "FIRST_QUEST_COMPLETE",
        "name": "첫 퀘스트 완료",
        "description": "퀘스트를 처음 완료하면 자동 지급되는 배지입니다.",
        "imageUrl": "/media/assets/badges/first_quest_complete.png",
        "criteria": "퀘스트 1회 완료",
        "earned": true,
        "earnedAt": "2026-05-24T08:30:00Z",
        "metadata": {
          "kind": "achievement_badge",
          "badgeCode": "FIRST_QUEST_COMPLETE",
          "criteria": "퀘스트 1회 완료",
          "sortOrder": 10
        }
      }
    ],
    "earnedCount": 1,
    "totalCount": 7
  }
}
```

프론트 처리:

- `earned === true`이면 획득 배지로 표시한다.
- `earnedAt`은 획득 시각이다. 미획득이면 `null`이다.
- 잠금 배지도 보여줄 화면이면 `earned === false` 항목을 잠금 처리하면 된다.

### 8.3 배지 상태 수동 동기화

기존 데이터에 대해 누락된 배지를 복구할 때 사용한다. 일반 화면 진입마다 호출할 필요는 없다.

```http
POST /api/users/me/badges/sync
Authorization: Bearer {accessToken}
```

응답 형태는 `GET /api/users/me/badges`와 같다.

### 8.4 상점 API에서 배지만 조회

상점 응답 구조를 재사용해야 하면 아래 필터도 지원한다.

```http
GET /api/shop/items?type=badge
Authorization: Bearer {accessToken}
```

`category`는 `badge`로 내려간다. 기존 `type=pass`에서는 획득 배지를 제외해서 패스 아이템과 섞이지 않게 했다.

---

## 9. 기록 페이지 통계 API

더미 데이터로 구성되어 있던 기록/통계 화면은 아래 API로 실제 데이터를 받을 수 있다.

```http
GET /api/records/stats?period=WEEKLY
Authorization: Bearer {accessToken}
```

`period` 값:

| 값 | 조회 범위 |
| --- | --- |
| `WEEKLY` | KST 기준 이번 주 월요일 ~ 일요일 |
| `MONTHLY` | KST 기준 이번 달 1일 ~ 말일 |
| `YEARLY` | KST 기준 올해 1월 1일 ~ 12월 31일 |

응답 구조:

```ts
type RecordStatsResponse = {
  period: 'WEEKLY' | 'MONTHLY' | 'YEARLY';
  startDate: string;
  endDate: string;
  summary: {
    averageConditionScore: number | null;
    averageConditionLevel: number | null;
    averageEnergyLevel: number | null;
    averageStressScore: number | null;
    exerciseCount: number;
    completedQuestCount: number;
    healthSyncedDays: number;
    improvementRatePercent: number;
    totalExerciseMinutes: number;
    totalSteps: number;
    totalDistanceMeters: number;
    totalActiveCaloriesKcal: number;
  };
  conditionTrend: Array<{
    date: string;
    label: string;
    conditionLevel: number | null;
    conditionScore: number | null;
    energyLevel: number | null;
    stressScore: number | null;
    exerciseMinutes: number;
    steps: number;
    distanceMeters: number;
    activeCaloriesKcal: number;
    completedQuestCount: number;
  }>;
  exerciseEffects: Array<{
    exerciseType: string;
    label: string;
    completedCount: number;
    exerciseMinutes: number;
    averageConditionScore: number | null;
    conditionDelta: number | null;
    averageStressScore: number | null;
  }>;
  dailyRecords: Array<{
    date: string;
    dayOfWeek: string;
    exerciseLabel: string;
    conditionLevel: number | null;
    conditionScore: number | null;
    energyLevel: number | null;
    stressScore: number | null;
    exerciseMinutes: number;
    steps: number;
    activeCaloriesKcal: number;
    completedQuestCount: number;
  }>;
  insight: {
    title: string;
    summary: string;
    recommendation: string;
  };
};
```

화면 매핑:

| 화면 영역 | 사용 필드 |
| --- | --- |
| 기간별 컨디션 변화 라인 차트 | `conditionTrend` |
| 평균 컨디션/운동 횟수/개선율 카드 | `summary` |
| 운동별 효과 분석 막대 차트 | `exerciseEffects` |
| 주간 운동 기록 표 | `dailyRecords` |
| AI 분석 인사이트 카드 | `insight` |

---

## 10. 개발 서버 반영 상태

- 개발 서버 DB에 `battle_match_queue` 테이블 생성 완료
- 개발 서버 DB에 `health_daily_summaries` 테이블 생성 완료
- 개발 서버 DB에 `user_item_effects` 테이블 생성 완료
- 개발 서버 DB에 획득 배지 seed 반영 완료: `db/20260524_seed_achievement_badges.sql`
- 개발 서버 백엔드 컨테이너 재배포 완료
- 실제 API 검증 완료
  - 첫 계정 호출: `message=Battle queued`, `matchStatus=WAITING`, `battleId=null`
  - 두 번째 계정 호출: `message=Battle matched`, `matchStatus=MATCHED`, `battleId` 생성
- 상점 패스 효과 코드는 `main`에 반영 완료. 개발 컨테이너에서 `/api/shop/items/{itemId}/use`가 보이면 즉시 연동 가능
- 획득 배지 코드는 `main`에 반영 완료. 개발 컨테이너 OpenAPI에서 `/api/users/me/badges`, `/api/users/me/badges/sync`, `/api/shop/items/{itemId}/use` 노출 확인 완료
