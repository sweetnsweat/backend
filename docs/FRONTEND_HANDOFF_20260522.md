# 2026-05-22 프론트 연동 가이드

이 문서는 2026-05-22 기준 프론트에서 새로 붙이면 되는 변경사항을 정리한다.

- 헬스 데이터 기반 퀘스트 완료: `feature/health-quest-verification` 브랜치 기준
- 배틀 API: `feature/battle-api` 브랜치 기준
- 상점 API: `main` 브랜치 기준
- 모든 인증 API는 `Authorization: Bearer {accessToken}` 필요
- 성공 응답은 공통 `ApiResponse<T>` 형태
- 에러 응답은 `ProblemDetail` 형태

---

## 1. 헬스 데이터 기반 퀘스트 완료

### 핵심 흐름

```text
GET /api/quests/today
-> data.verificationWindow.startTime 저장
-> 사용자가 운동 수행
-> 완료 버튼 클릭
-> Health Connect readRecords(startTime=verificationWindow.startTime, endTime=now)
-> PATCH /api/quests/{questId}/complete with healthSamples
-> 항상 COMPLETED 응답, 백엔드가 VERIFIED 또는 MANUAL로 분류
```

### 오늘 퀘스트 조회

```http
GET /api/quests/today
Authorization: Bearer {accessToken}
```

`data.verificationWindow`가 추가된다.

```json
{
  "success": true,
  "code": "OK",
  "message": "오늘 퀘스트를 조회했습니다.",
  "data": {
    "id": 12,
    "questDate": "2026-05-22",
    "questType": "ROUTINE",
    "targetMetric": "ROUTINE",
    "status": "ISSUED",
    "completed": false,
    "title": "오늘 루틴 완료",
    "targetValue": 1,
    "progressValue": 0,
    "rewardExp": 30,
    "rewardGold": 15,
    "verificationWindow": {
      "startTime": "2026-05-22T05:08:40.534Z",
      "endTime": null
    },
    "exercises": [
      {
        "exerciseId": 1,
        "exerciseName": "Squat",
        "category": "strength",
        "seq": 1,
        "targetSets": 3,
        "targetReps": 10,
        "targetDurationSec": null
      }
    ]
  }
}
```

프론트는 완료 버튼을 누를 때 아래 구간으로 Health Connect 데이터를 읽으면 된다.

```text
startTime = data.verificationWindow.startTime
endTime = new Date().toISOString()
```

### 완료 요청

```http
PATCH /api/quests/{questId}/complete
Authorization: Bearer {accessToken}
Content-Type: application/json
```

기본 요청은 JSON이다. 건강 데이터를 아직 못 읽었으면 빈 JSON `{}`를 보내면 되고, 백엔드는 `MANUAL` 완료로 처리한다.

모바일 클라이언트 호환을 위해 빈 `application/x-www-form-urlencoded` 요청도 받는다. 이 경우도 `MANUAL` 완료로 처리된다. 다만 신규 프론트 코드는 `Content-Type: application/json`으로 맞춘다.

지원하지 않는 Content-Type은 `415 UNSUPPORTED_MEDIA_TYPE`으로 내려간다. 에러 응답에는 `code`, `path`, `contentType`, `supportedMediaTypes`가 포함된다.

요청 타입:

```ts
type CompleteQuestRequest = {
  progressValue?: number;
  proof?: Record<string, unknown>;
  healthSamples?: HealthMetricSampleRequest[];
};

type HealthMetricSampleRequest = {
  type?: string;
  value: number;
  unit?: string;
  startTime: string;
  endTime?: string;
  source: 'health_connect' | string;
  dataOrigin?: string;
  rawRecordType?: string;
};
```

완료 버튼은 하나만 둔다. 프론트는 가능한 경우 Health Connect 데이터를 읽어서 `healthSamples`에 담아 보내고, 백엔드가 완료 유형을 결정한다.

### 완료 응답 분기

건강 데이터가 충분하면 `VERIFIED` 완료다. 기존 퀘스트 보상을 지급하고 배틀 점수에 반영된다.

```json
{
  "success": true,
  "code": "OK",
  "message": "퀘스트가 완료되었습니다.",
  "data": {
    "status": "COMPLETED",
    "completed": true,
    "completionType": "VERIFIED",
    "verificationStatus": "VERIFIED",
    "battleEligible": true,
    "rewardExp": 30,
    "rewardGold": 15
  }
}
```

건강 데이터가 없거나 부족하면 `MANUAL` 완료다. 습관 유지를 위해 완료는 인정하지만, 축소 보상만 지급하고 배틀 점수에는 반영하지 않는다.

```json
{
  "success": true,
  "code": "OK",
  "message": "퀘스트가 완료되었습니다.",
  "data": {
    "status": "COMPLETED",
    "completed": true,
    "completionType": "MANUAL",
    "verificationStatus": "INSUFFICIENT_DATA",
    "battleEligible": false,
    "rewardExp": 10,
    "rewardGold": 5
  }
}
```

`verificationStatus` 값:

```text
VERIFIED          건강 데이터 검증 성공
NOT_PROVIDED      healthSamples 미전송
INSUFFICIENT_DATA healthSamples는 있었지만 기준 부족
```

권장 `type`:

```text
ExerciseSession
Steps
Distance
ActiveCaloriesBurned
TotalCaloriesBurned
HeartRate
Speed
Power
```

근력/홈트 예시:

```json
{
  "healthSamples": [
    {
      "type": "ExerciseSession",
      "value": 26,
      "unit": "minutes",
      "startTime": "2026-05-22T05:30:00Z",
      "endTime": "2026-05-22T05:56:00Z",
      "source": "health_connect",
      "dataOrigin": "com.sec.android.app.shealth",
      "rawRecordType": "StrengthTraining"
    },
    {
      "type": "ActiveCaloriesBurned",
      "value": 160,
      "unit": "kcal",
      "startTime": "2026-05-22T05:30:00Z",
      "endTime": "2026-05-22T05:56:00Z",
      "source": "health_connect",
      "dataOrigin": "com.sec.android.app.shealth",
      "rawRecordType": "ActiveCaloriesBurned"
    },
    {
      "type": "HeartRate",
      "value": 128,
      "unit": "bpm",
      "startTime": "2026-05-22T05:40:00Z",
      "endTime": "2026-05-22T05:40:00Z",
      "source": "health_connect",
      "dataOrigin": "com.sec.android.app.shealth",
      "rawRecordType": "HeartRate"
    }
  ]
}
```

러닝 예시:

```json
{
  "healthSamples": [
    {
      "type": "ExerciseSession",
      "value": 28,
      "unit": "minutes",
      "startTime": "2026-05-22T05:30:00Z",
      "endTime": "2026-05-22T05:58:00Z",
      "source": "health_connect",
      "dataOrigin": "com.sec.android.app.shealth",
      "rawRecordType": "Running"
    },
    {
      "type": "Distance",
      "value": 3.2,
      "unit": "km",
      "startTime": "2026-05-22T05:30:00Z",
      "endTime": "2026-05-22T05:58:00Z",
      "source": "health_connect",
      "dataOrigin": "com.sec.android.app.shealth",
      "rawRecordType": "Distance"
    },
    {
      "type": "HeartRate",
      "value": 148,
      "unit": "bpm",
      "startTime": "2026-05-22T05:45:00Z",
      "endTime": "2026-05-22T05:45:00Z",
      "source": "health_connect",
      "dataOrigin": "com.sec.android.app.shealth",
      "rawRecordType": "HeartRate"
    }
  ]
}
```

### 읽어야 하는 Health Connect RecordType

| 운동 | 권장 RecordType |
| --- | --- |
| 걷기 | `Steps`, `Distance`, `ExerciseSession` |
| 러닝/조깅 | `ExerciseSession`, `Distance`, `HeartRate`, `ActiveCaloriesBurned` |
| 자전거 | `ExerciseSession`, `Distance`, `Speed`, `Power`, `HeartRate` |
| 수영 | `ExerciseSession`, `Distance`, `HeartRate` |
| 근력/홈트 | `ExerciseSession`, `ActiveCaloriesBurned`, `HeartRate` |
| 요가/스트레칭/회복 | `ExerciseSession` |

### 기존 수동 완료 호환

헬스 기반 완료가 기본이지만 기존 수동 완료 요청도 호환된다. 이 경우 `completionType=MANUAL`, `battleEligible=false`로 처리된다.

```json
{
  "progressValue": 1,
  "proof": {
    "source": "manual"
  }
}
```

---

## 2. 배틀 API

배틀은 현재 모바일 화면 흐름에 맞춰 `로비 -> 매칭 -> 상세 -> 결과` 순서로 붙이면 된다.

점수 기준:

- `DAILY`: KST 오늘 00:00부터 내일 00:00 직전까지의 `battleEligible=true` 완료 퀘스트와 헬스 데이터 증거 합산
- `WEEKLY`: KST 월요일부터 일요일까지의 `battleEligible=true` 완료 퀘스트와 헬스 데이터 증거 합산
- 승패 기준은 퀘스트 EXP가 아니라 `TOTAL_SCORE`다.
- `battleEligible=true`인 검증 완료 퀘스트만 기본 점수와 Health Connect 운동 지표가 배틀 점수에 반영된다.
- `completionType=MANUAL`인 수동 완료 퀘스트는 배틀 점수와 랭킹에 반영하지 않는다.

현재 배틀 점수 계산:

```text
TOTAL_SCORE =
  배틀 반영 가능 완료 퀘스트 수 * 100
+ 헬스 검증 성공 퀘스트 수 * 50
+ 운동 시간(분) * 10
+ 이동 거리(m) * 0.03
+ 걸음 수 * 0.01
+ 활동 칼로리(kcal) * 2
```

매칭 기준:

- 같은 기간/모드에 이미 진행 중인 배틀이 있으면 기존 배틀 반환
- 없으면 내 점수와 가장 가까운 실제 active 유저와 즉시 매칭
- 상대가 없으면 `409 BATTLE_OPPONENT_NOT_FOUND`

### 2.1 배틀 로비 요약

```http
GET /api/battles/me/summary
Authorization: Bearer {accessToken}
```

응답:

```json
{
  "success": true,
  "code": "OK",
  "message": "Request succeeded",
  "data": {
    "rankName": "Unranked",
    "wins": 0,
    "losses": 0,
    "draws": 0,
    "winRate": 0,
    "currentDailyBattle": null,
    "currentWeeklyBattle": null
  }
}
```

진행 중 배틀이 있으면:

```json
{
  "battleId": 1,
  "mode": "DAILY",
  "status": "ACTIVE",
  "periodStartDate": "2026-05-22",
  "periodEndDate": "2026-05-22",
  "endsAt": "2026-05-22T15:00:00Z"
}
```

모바일 적용:

- `BattleLobbyScreen` 진입 시 호출
- mock 승/패/승률/랭크 제거
- `currentDailyBattle` 또는 `currentWeeklyBattle`이 있으면 "이어하기" 처리 가능

### 2.2 배틀 매칭 시작

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

`mode`는 `DAILY`, `WEEKLY` 중 하나.

성공 응답은 배틀 상세와 동일하다.

```json
{
  "success": true,
  "code": "OK",
  "message": "Battle matched",
  "data": {
    "battleId": 1,
    "mode": "DAILY",
    "status": "ACTIVE",
    "periodStartDate": "2026-05-22",
    "periodEndDate": "2026-05-22",
    "startsAt": "2026-05-21T15:00:00Z",
    "endsAt": "2026-05-22T15:00:00Z",
    "remainingSeconds": 3600,
    "participants": [
      {
        "userId": 10,
        "nickname": "나",
        "profileImageUrl": "https://example.com/me.png",
        "me": true,
        "score": 980,
        "result": "PENDING"
      },
      {
        "userId": 11,
        "nickname": "상대",
        "profileImageUrl": "https://example.com/opponent.png",
        "me": false,
        "score": 779,
        "result": "PENDING"
      }
    ],
    "score": {
      "myScore": 980,
      "opponentScore": 779,
      "leadingUserId": 10
    },
    "metrics": [
      {
        "metricKey": "TOTAL_SCORE",
        "label": "배틀 점수",
        "myValue": "980점",
        "myPercent": 100,
        "opponentValue": "779점",
        "opponentPercent": 79,
        "unit": "점"
      },
      {
        "metricKey": "ACTIVE_MINUTES",
        "label": "운동 시간",
        "myValue": "30분",
        "myPercent": 100,
        "opponentValue": "25분",
        "opponentPercent": 83,
        "unit": "분"
      },
      {
        "metricKey": "DISTANCE",
        "label": "이동 거리",
        "myValue": "3000m",
        "myPercent": 100,
        "opponentValue": "1800m",
        "opponentPercent": 60,
        "unit": "m"
      }
    ]
  }
}
```

상대가 없을 때:

```json
{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "detail": "매칭 가능한 상대가 없습니다.",
  "code": "BATTLE_OPPONENT_NOT_FOUND",
  "path": "/api/battles/match"
}
```

모바일 적용:

- `BattleMatchingScreen`에서 기존 2.8초 mock 타이머 대신 이 API 호출
- 응답의 `battleId`, `participants`, `mode`를 `BattleScreen`으로 전달
- 같은 기간에 재호출해도 새 배틀이 아니라 기존 배틀이 내려온다

### 2.3 배틀 상세 조회

```http
GET /api/battles/{battleId}
Authorization: Bearer {accessToken}
```

응답은 `POST /api/battles/match`의 `data`와 동일한 `BattleDetailResponse`.

모바일 적용:

- `BattleScreen` 진입 시 호출
- `participants.find(p => p.me)`를 내 정보로 사용
- `participants.find(p => !p.me)`를 상대 정보로 사용
- 비교 바는 `metrics` 배열을 그대로 렌더링
- `remainingSeconds`로 남은 시간 표시 가능

### 2.4 배틀 결과 조회

```http
GET /api/battles/{battleId}/result
Authorization: Bearer {accessToken}
```

응답:

```json
{
  "success": true,
  "code": "OK",
  "message": "Request succeeded",
  "data": {
    "battleId": 1,
    "mode": "DAILY",
    "status": "FINALIZED",
    "periodStartDate": "2026-05-22",
    "periodEndDate": "2026-05-22",
    "startsAt": "2026-05-21T15:00:00Z",
    "endsAt": "2026-05-22T15:00:00Z",
    "finalized": true,
    "result": "WIN",
    "winnerUserId": 10,
    "rewardExp": 30,
    "rewardGold": 15,
    "myScore": 980,
    "opponentScore": 367,
    "participants": [],
    "metrics": []
  }
}
```

`result` 값:

```text
PENDING
WIN
LOSS
DRAW
```

규칙:

- 기간 종료 전에도 호출 가능하며 현재 기준 결과를 내려준다.
- 기간 종료 전이면 `finalized=false`.
- 기간 종료 후 최초 결과 조회 시 서버가 결과를 확정하고 `finalized=true`.
- 승리자에게만 보상이 지급된다. `DAILY` 승리 보상은 EXP 30 / Gold 15, `WEEKLY` 승리 보상은 EXP 100 / Gold 50이다.
- 패배와 무승부는 보상이 없으며 `rewardExp=0`, `rewardGold=0`으로 내려간다.
- 같은 배틀 승리 보상은 한 번만 지급된다. 결과를 다시 조회해도 중복 지급되지 않는다.

모바일 적용:

- `BattleResultScreen`의 `won: boolean` route param 제거 권장
- 서버의 `result`로 승/패/무승부 UI 분기
- 승리 화면에서는 `rewardExp`, `rewardGold`를 보상 표시값으로 사용

### 2.5 배틀 기록 조회

```http
GET /api/battles/history?page=0&size=20
Authorization: Bearer {accessToken}
```

응답:

```json
{
  "success": true,
  "code": "OK",
  "message": "Request succeeded",
  "data": {
    "page": 0,
    "size": 20,
    "totalCount": 1,
    "totalPages": 1,
    "first": true,
    "last": true,
    "hasNext": false,
    "nextPage": null,
    "battles": [
      {
        "battleId": 1,
        "mode": "DAILY",
        "result": "WIN",
        "periodStartDate": "2026-05-22",
        "periodEndDate": "2026-05-22",
        "endedAt": "2026-05-22T15:00:10Z",
        "opponent": {
          "userId": 11,
          "nickname": "상대",
          "profileImageUrl": null,
          "me": false,
          "score": 20,
          "result": "LOSS"
        },
        "myScore": 80,
        "opponentScore": 20
      }
    ]
  }
}
```

모바일 적용:

- 지금 배틀 화면에는 필수는 아니지만, 나중에 로비 하단 기록/전적 목록에 바로 사용 가능

### 배틀 TypeScript 타입

```ts
type BattleMode = 'DAILY' | 'WEEKLY';
type BattleStatus = 'ACTIVE' | 'FINALIZED' | 'CANCELLED';
type BattleResult = 'PENDING' | 'WIN' | 'LOSS' | 'DRAW';

type BattleParticipant = {
  userId: number;
  nickname: string;
  profileImageUrl: string | null;
  me: boolean;
  score: number;
  result: BattleResult;
};

type BattleMetric = {
  metricKey:
    | 'TOTAL_SCORE'
    | 'ACTIVE_MINUTES'
    | 'DISTANCE'
    | 'STEPS'
    | 'ACTIVE_CALORIES'
    | 'COMPLETED_QUESTS'
    | string;
  label: string;
  myValue: string;
  myPercent: number;
  opponentValue: string;
  opponentPercent: number;
  unit: string;
};

type BattleDetail = {
  battleId: number;
  mode: BattleMode;
  status: BattleStatus;
  periodStartDate: string;
  periodEndDate: string;
  startsAt: string;
  endsAt: string;
  remainingSeconds: number;
  participants: BattleParticipant[];
  score: {
    myScore: number;
    opponentScore: number;
    leadingUserId: number | null;
  };
  metrics: BattleMetric[];
};
```

---

## 3. 상점 API

상점은 기존 DB 구조를 유지하면서 모바일 화면의 `character`, `pass` 탭에 맞게 응답을 확장했다.

DB 저장값과 모바일 탭 매핑:

```text
character -> itemType skin, profile
pass      -> itemType ticket, pvp_badge, gift, consumable
```

### 3.1 상점 아이템 목록

```http
GET /api/shop/items?type=character
Authorization: Bearer {accessToken}
```

`type`은 선택값이다.

- 생략: 전체 활성 아이템
- `character`: 캐릭터/프로필 아이템
- `pass`: 이용권/배지/소모품 아이템
- 기존 DB 타입인 `skin`, `profile`, `ticket`, `pvp_badge`, `gift`, `consumable`도 그대로 사용 가능

응답:

```json
{
  "success": true,
  "code": "OK",
  "message": "Request succeeded",
  "data": {
    "items": [
      {
        "id": 17,
        "itemType": "skin",
        "category": "character",
        "name": "이수연",
        "description": "체대 입시생 · 인내력",
        "priceCurrency": 0,
        "sellable": true,
        "owned": true,
        "ownedQuantity": 1,
        "purchasable": true,
        "equipped": true,
        "special": false,
        "effect": "기본 캐릭터",
        "imageUrl": "https://i.imgur.com/v0njcuh.png",
        "metadata": {
          "bg": ["#fce7f3", "#ffe4e6"],
          "effect": "기본 캐릭터",
          "special": false
        }
      }
    ],
    "balanceCurrency": 1200
  }
}
```

주요 필드:

- `category`: 모바일 탭 구분용. `character` 또는 `pass`
- `owned`: 현재 사용자가 보유했는지
- `ownedQuantity`: 보유 수량
- `purchasable`: 현재 잔액으로 구매 가능한지
- `equipped`: 현재 장착 중인지
- `special`: 스페셜 표시 여부
- `effect`: 효과/라벨 표시 문구
- `balanceCurrency`: 현재 골드 잔액

### 3.2 아이템 구매

```http
POST /api/shop/items/{itemId}/purchase
Authorization: Bearer {accessToken}
Content-Type: application/json
```

요청:

```json
{
  "quantity": 1
}
```

응답:

```json
{
  "success": true,
  "code": "OK",
  "message": "아이템을 구매했습니다.",
  "data": {
    "item": {
      "id": 427,
      "itemId": 15,
      "itemType": "skin",
      "name": "카일린",
      "description": "여우 도적 · 기민함",
      "quantity": 1,
      "imageUrl": "",
      "metadata": {
        "effect": "캐릭터 스킨",
        "special": false
      }
    },
    "balanceCurrency": 850,
    "transaction": {
      "txType": "purchase",
      "amount": -350
    }
  }
}
```

구매 후 프론트는 목록을 다시 조회해서 `owned`, `ownedQuantity`, `purchasable`, `balanceCurrency`를 갱신하면 된다.

잔액이 부족하면:

```json
{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "detail": "잔액이 부족합니다.",
  "code": "INSUFFICIENT_BALANCE",
  "path": "/api/shop/items/15/purchase"
}
```

### 3.3 아이템 장착

```http
POST /api/shop/items/{itemId}/equip
Authorization: Bearer {accessToken}
```

응답:

```json
{
  "success": true,
  "code": "OK",
  "message": "아이템을 장착했습니다.",
  "data": {
    "item": {
      "id": 306,
      "itemId": 17,
      "itemType": "skin",
      "name": "이수연",
      "description": "체대 입시생 · 인내력",
      "quantity": 1,
      "imageUrl": "https://i.imgur.com/v0njcuh.png",
      "metadata": {
        "effect": "기본 캐릭터",
        "special": false
      }
    },
    "profileImageUrl": "https://i.imgur.com/v0njcuh.png"
  }
}
```

현재 장착 상태는 `GET /api/shop/items?type=character`의 `equipped`로 확인한다.

보유하지 않은 아이템을 장착하면:

```json
{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "detail": "보유하지 않은 아이템입니다.",
  "code": "ITEM_NOT_OWNED",
  "path": "/api/shop/items/17/equip"
}
```

### 상점 TypeScript 타입

```ts
type ShopCategory = 'character' | 'pass';

type ShopItem = {
  id: number;
  itemType: string;
  category: ShopCategory;
  name: string;
  description: string | null;
  priceCurrency: number;
  sellable: boolean;
  owned: boolean;
  ownedQuantity: number;
  purchasable: boolean;
  equipped: boolean;
  special: boolean;
  effect: string | null;
  imageUrl: string | null;
  metadata: Record<string, unknown>;
};

type ShopItemList = {
  items: ShopItem[];
  balanceCurrency: number;
};

type PurchaseItemRequest = {
  quantity?: number;
};
```

모바일 적용:

- `ShopScreen` 진입 시 `GET /api/shop/items?type=character`, `GET /api/shop/items?type=pass` 호출
- 캐릭터 탭은 `category === 'character'` 목록 사용
- 아이템/패스 탭은 `category === 'pass'` 목록 사용
- 구매 버튼은 `owned`, `purchasable`, `priceCurrency` 기준으로 분기
- 장착 버튼은 `owned && !equipped`일 때 노출
- 패스류는 `ownedQuantity`를 수량 뱃지로 표시 가능

---

## 4. FCM 1차 이벤트 알림

FCM token 등록 흐름은 기존과 동일하다.

```http
POST /api/push-tokens
Authorization: Bearer {accessToken}
Content-Type: application/json
```

```json
{
  "token": "{fcmRegistrationToken}",
  "platform": "android",
  "deviceId": "{optionalDeviceId}"
}
```

백엔드가 아래 3가지 이벤트를 자동 발송한다.

| type | 발생 시점 | 프론트 이동 |
| --- | --- | --- |
| `BATTLE_MATCHED` | 새 배틀 매칭 생성 직후 | 배틀 상세/진행 화면 |
| `BATTLE_RESULT_READY` | 배틀 결과가 최초 확정된 직후 | 배틀 결과 화면 |
| `WEEKLY_STATS_READY` | 매주 월요일 오전 9시 KST | 주간 통계 화면 |

### 공통 수신 처리

프론트는 FCM `data.type` 기준으로 라우팅하면 된다.

```ts
type PushNotificationType =
  | 'BATTLE_MATCHED'
  | 'BATTLE_RESULT_READY'
  | 'WEEKLY_STATS_READY';

type PushNotificationData = {
  type: PushNotificationType;
  route: string;
  battleId?: string;
  battleMode?: 'DAILY' | 'WEEKLY';
  result?: 'WIN' | 'LOSS' | 'DRAW' | 'PENDING';
  weekStartDate?: string;
  weekEndDate?: string;
};
```

### 배틀 매칭 완료

```json
{
  "type": "BATTLE_MATCHED",
  "route": "battle/detail",
  "battleId": "12",
  "battleMode": "DAILY"
}
```

새 배틀이 생성된 경우에만 발송된다. 이미 진행 중인 배틀을 다시 조회하는 경우에는 중복 발송되지 않는다.

### 배틀 결과 확정

```json
{
  "type": "BATTLE_RESULT_READY",
  "route": "battle/result",
  "battleId": "12",
  "battleMode": "DAILY",
  "result": "WIN"
}
```

배틀 종료 시간이 지난 뒤 결과가 최초 확정되는 순간에만 발송된다. 이미 확정된 결과를 다시 조회하는 경우에는 중복 발송되지 않는다.

### 주간 통계 준비 완료

```json
{
  "type": "WEEKLY_STATS_READY",
  "route": "stats/weekly",
  "weekStartDate": "2026-05-11",
  "weekEndDate": "2026-05-17"
}
```

매주 월요일 오전 9시 KST에 지난주 월요일-일요일 통계를 기준으로 발송된다.

알림 설정 기준:

- 배틀 알림: `pushEnabled=true` and `pushCompetitionEnabled=true`
- 주간 통계 알림: `pushEnabled=true` and `pushRoutineEnabled=true`

---

## 5. 프론트 구현 순서 권장

1. `QuestService.completeQuest` 요청 타입에 `healthSamples` 추가
2. 오늘 퀘스트 응답 타입에 `verificationWindow` 추가
3. 완료 버튼 클릭 시 Health Connect 읽기 후 `healthSamples` 전송
4. 같은 계정으로 완료 테스트를 다시 해야 하면 `POST /api/quests/{questId}/reset` 호출
5. `BattleService.ts` 새로 추가
6. `BattleLobbyScreen`에서 `GET /api/battles/me/summary` 연동
7. `BattleMatchingScreen`에서 `POST /api/battles/match` 연동
8. `BattleScreen`에서 `GET /api/battles/{battleId}` 연동
9. `BattleResultScreen`에서 `GET /api/battles/{battleId}/result` 연동
10. `ShopService.ts`에서 `GET /api/shop/items?type=character`, `GET /api/shop/items?type=pass` 연동
11. `ShopScreen` 구매/장착 버튼을 `POST /api/shop/items/{itemId}/purchase`, `POST /api/shop/items/{itemId}/equip`에 연결
12. FCM 수신 핸들러에서 `data.type`별 화면 이동 처리

## 6. 주의사항

- 헬스 퀘스트 완료는 Health Connect 동기화 지연 때문에 같은 요청을 몇 분 뒤 재시도할 수 있게 만들어야 한다.
- `POST /api/quests/{questId}/reset`은 개발/테스트용이다. 완료 상태, 완료 증거, 퀘스트 완료 보상을 되돌려 같은 계정으로 완료 API를 다시 테스트할 수 있다.
- 배틀 점수는 퀘스트 EXP 기준이 아니라 활동량 기반 `TOTAL_SCORE` 기준이다.
- 배틀 결과는 기간 종료 전에도 볼 수 있지만, 그때는 `finalized=false`라 최종 결과가 아니다.
- 배틀 매칭 상대가 없는 경우는 정상 케이스로 보고 프론트에서 안내 문구를 보여주면 된다.
- 상점의 `character`, `pass`는 프론트 편의용 분류다. DB 원본 타입은 `itemType`에 그대로 내려간다.
- 상점 구매/장착 후에는 목록을 재조회해서 잔액과 보유/장착 상태를 동기화하는 방식이 가장 단순하다.
- FCM은 앱 밖 사용자를 다시 진입시키는 용도다. 퀘스트 완료처럼 앱 안에서 즉시 결과를 보는 흐름은 원격 푸시 대상이 아니다.
