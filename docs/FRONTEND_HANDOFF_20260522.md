# 2026-05-22 프론트 연동 가이드

이 문서는 2026-05-22 기준 프론트에서 새로 붙이면 되는 변경사항을 정리한다.

- 헬스 데이터 기반 퀘스트 완료: `feature/health-quest-verification` 브랜치 기준
- 배틀 API: `feature/battle-api` 브랜치 기준
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
-> 성공 시 COMPLETED, 실패 시 INSUFFICIENT_HEALTH_PROOF
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

### 실패 응답

건강 데이터가 부족하면 완료/보상이 처리되지 않는다.

```json
{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "detail": "퀘스트 완료를 인정할 건강 데이터가 부족합니다.",
  "code": "INSUFFICIENT_HEALTH_PROOF",
  "path": "/api/quests/12/complete"
}
```

프론트 처리:

- Health Connect 권한이 없으면 권한 요청 화면으로 유도
- 데이터가 부족하면 "운동 데이터가 아직 동기화되지 않았어요. 잠시 후 다시 시도해 주세요." 표시
- 재시도 버튼으로 같은 `questId`에 다시 완료 요청

### 기존 수동 완료 호환

헬스 기반 완료가 기본이지만 기존 수동 완료 요청도 호환된다.

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

- `DAILY`: KST 오늘 00:00부터 내일 00:00 직전까지의 완료 퀘스트와 헬스 데이터 증거 합산
- `WEEKLY`: KST 월요일부터 일요일까지의 완료 퀘스트와 헬스 데이터 증거 합산
- 승패 기준은 퀘스트 EXP가 아니라 `TOTAL_SCORE`다.
- 완료 퀘스트는 기본 점수를 주고, Health Connect 검증으로 저장된 운동 시간/거리/걸음/활동 칼로리가 점수를 더 만든다.

현재 배틀 점수 계산:

```text
TOTAL_SCORE =
  완료 퀘스트 수 * 100
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

모바일 적용:

- `BattleResultScreen`의 `won: boolean` route param 제거 권장
- 서버의 `result`로 승/패/무승부 UI 분기

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

## 3. 프론트 구현 순서 권장

1. `QuestService.completeQuest` 요청 타입에 `healthSamples` 추가
2. 오늘 퀘스트 응답 타입에 `verificationWindow` 추가
3. 완료 버튼 클릭 시 Health Connect 읽기 후 `healthSamples` 전송
4. `BattleService.ts` 새로 추가
5. `BattleLobbyScreen`에서 `GET /api/battles/me/summary` 연동
6. `BattleMatchingScreen`에서 `POST /api/battles/match` 연동
7. `BattleScreen`에서 `GET /api/battles/{battleId}` 연동
8. `BattleResultScreen`에서 `GET /api/battles/{battleId}/result` 연동

## 4. 주의사항

- 헬스 퀘스트 완료는 Health Connect 동기화 지연 때문에 같은 요청을 몇 분 뒤 재시도할 수 있게 만들어야 한다.
- 배틀 점수는 퀘스트 EXP 기준이 아니라 활동량 기반 `TOTAL_SCORE` 기준이다.
- 배틀 결과는 기간 종료 전에도 볼 수 있지만, 그때는 `finalized=false`라 최종 결과가 아니다.
- 배틀 매칭 상대가 없는 경우는 정상 케이스로 보고 프론트에서 안내 문구를 보여주면 된다.
