# 헬스 데이터 기반 퀘스트 완료 프론트 연동 가이드

## 목적

퀘스트 완료 버튼을 눌렀을 때 사용자가 실제로 운동했는지 Health Connect 데이터로 검증한다.

프론트는 백엔드가 내려준 검증 시간창에 맞춰 Health Connect 데이터를 읽고, `PATCH /api/quests/{questId}/complete` 요청의 `healthSamples`에 담아 보낸다. 백엔드는 운동 카테고리별 룰로 검증한 뒤 완료와 보상 지급 여부를 결정한다.

## 전체 흐름

```text
GET /api/quests/today
-> 응답의 verificationWindow.startTime 저장
-> 사용자가 운동 수행
-> 사용자가 완료 버튼 클릭
-> Health Connect readRecords(startTime=verificationWindow.startTime, endTime=now)
-> PATCH /api/quests/{questId}/complete with healthSamples
-> 성공 시 COMPLETED, 실패 시 INSUFFICIENT_HEALTH_PROOF
```

## 오늘 퀘스트 조회

```http
GET /api/quests/today
Authorization: Bearer {accessToken}
```

응답에 `verificationWindow`가 추가된다.

```json
{
  "data": {
    "id": 12,
    "questType": "ROUTINE",
    "targetMetric": "ROUTINE",
    "targetValue": 1,
    "sessionType": "full_body",
    "verificationWindow": {
      "startTime": "2026-05-22T05:08:40.534Z",
      "endTime": null
    },
    "exercises": [
      {
        "exerciseName": "Squat",
        "category": "strength",
        "targetSets": 3,
        "targetReps": 10
      }
    ]
  }
}
```

프론트는 완료 버튼을 누를 때 아래 구간으로 Health Connect를 읽는다.

```text
startTime = data.verificationWindow.startTime
endTime = new Date().toISOString()
```

`endTime`은 `null`로 내려간다. 완료 요청 시점마다 현재 시각을 넣으면 된다.

## 읽어야 하는 Health Connect RecordType

1차 구현에서는 아래 타입만 보내도 된다.

| RecordType | 백엔드 metric | 용도 |
| --- | --- | --- |
| `ExerciseSession` | `EXERCISE_SESSION` | 운동 시간, 운동 세션 타입 |
| `Steps` | `STEPS` | 걷기 완료 검증 |
| `Distance` | `DISTANCE` | 러닝/자전거/수영/걷기 거리 검증 |
| `ActiveCaloriesBurned` | `ACTIVE_CALORIES_BURNED` | 근력/홈트/유산소 보조 검증 |
| `TotalCaloriesBurned` | `TOTAL_CALORIES_BURNED` | 칼로리 보조 검증 |
| `HeartRate` | `HEART_RATE` | 러닝/유산소/근력 보조 신뢰도 |
| `Speed` | `SPEED` | 현재는 저장만 가능, 완료 판정 주 지표는 아님 |
| `Power` | `POWER` | 현재는 저장만 가능, 자전거 보조 확장용 |

권장 최소 조합:

- 걷기: `Steps`, `Distance`, `ExerciseSession`
- 러닝/조깅: `ExerciseSession`, `Distance`, `HeartRate`, `ActiveCaloriesBurned`
- 자전거: `ExerciseSession`, `Distance`, `Speed`, `Power`, `HeartRate`
- 수영: `ExerciseSession`, `Distance`, `HeartRate`
- 근력/홈트: `ExerciseSession`, `ActiveCaloriesBurned`, `HeartRate`
- 요가/스트레칭/회복: `ExerciseSession`

## healthSamples 요청 형식

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
    }
  ]
}
```

필드 설명:

| 필드 | 필수 | 설명 |
| --- | --- | --- |
| `type` | 권장 | 공통 타입 이름. 없으면 `rawRecordType`으로 백엔드가 추론한다. |
| `value` | 필수 | 수치 값 |
| `unit` | 권장 | `count`, `m`, `km`, `kcal`, `bpm`, `minutes`, `seconds` 등 |
| `startTime` | 필수 | 데이터 시작 시각, ISO-8601 |
| `endTime` | 권장 | 데이터 종료 시각, ISO-8601 |
| `source` | 필수 | Android는 `health_connect` |
| `dataOrigin` | 권장 | 예: `com.sec.android.app.shealth`, `com.google.android.apps.fitness` |
| `rawRecordType` | 권장 | Health Connect 원본 record type 또는 운동 세션 이름 |

`type`은 아래 값 중 하나를 쓰는 것을 권장한다.

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

## 완료 요청 예시

```http
PATCH /api/quests/12/complete
Authorization: Bearer {accessToken}
Content-Type: application/json
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

## 검증 시간창 규칙

백엔드는 완료 요청 시점에 아래 시간창만 인정한다.

```text
start = quest.createdAt - 5분
end = complete 요청 시각 + 2분
```

프론트는 `verificationWindow.startTime`부터 현재 시각까지 읽으면 된다.

주의:

- 퀘스트 발급 전에 한 운동은 대부분 제외된다.
- 완료 버튼 이후의 미래 데이터는 인정되지 않는다.
- Health Connect 동기화가 늦어서 데이터가 없으면 실패할 수 있다. 이 경우 몇 분 뒤 다시 완료 요청하면 된다.
- 긴 세션이 시간창에 일부만 걸치면 백엔드가 겹친 구간만 비율 계산한다.

## 운동 카테고리별 백엔드 판정 기준

| 카테고리 | 주 지표 | 보조 지표 | 특징 |
| --- | --- | --- | --- |
| 걷기 | `STEPS` | `DISTANCE`, `EXERCISE_SESSION` | 걸음 수 중심 |
| 러닝/조깅 | `DISTANCE` | `EXERCISE_SESSION`, `HEART_RATE`, `ACTIVE_CALORIES_BURNED` | 거리/시간 중심, 심박은 신뢰도 보조 |
| 자전거 | `DISTANCE` | `EXERCISE_SESSION`, `SPEED`, `POWER`, `HEART_RATE` | 거리/시간 중심 |
| 수영 | `DISTANCE`, `EXERCISE_SESSION` | `HEART_RATE`, `ACTIVE_CALORIES_BURNED` | 세션 타입이 중요 |
| 근력/홈트 | `EXERCISE_SESSION` | `ACTIVE_CALORIES_BURNED`, `HEART_RATE` | 걸음 수는 주 기준으로 보지 않음 |
| 요가/스트레칭/회복 | `EXERCISE_SESSION` | 없음 | 심박 상승을 강제하지 않음 |

대략 목표의 80% 이상을 수행하면 완료 가능성이 높다.
활동 칼로리는 보조 지표라 70% 이상부터 인정에 사용한다.

## 성공 응답

완료 성공 시 기존 퀘스트 응답과 동일하게 내려간다.

```json
{
  "success": true,
  "code": "OK",
  "message": "퀘스트가 완료되었습니다.",
  "data": {
    "id": 12,
    "status": "COMPLETED",
    "completed": true,
    "progressValue": 1,
    "rewardExp": 30,
    "rewardGold": 15,
    "completedAt": "2026-05-22T05:59:10Z"
  }
}
```

백엔드는 내부적으로 `proof_json`에 아래 검증 결과를 저장한다.

```json
{
  "source": "health_data",
  "verified": true,
  "confidence": 0.86,
  "rule": "strength_health_proof",
  "matchedMetrics": ["exercise_duration", "active_calories", "heart_rate"],
  "reason": "근력 퀘스트 수행 증거가 확인되었습니다.",
  "verificationWindow": {},
  "metrics": {}
}
```

현재 완료 응답에는 `proof_json`을 직접 내려주지 않는다.

## 실패 응답

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

프론트 처리 권장:

- 사용자에게 "운동 데이터가 아직 동기화되지 않았어요. 잠시 후 다시 시도해 주세요." 표시
- Health Connect 권한이 없으면 권한 요청 화면으로 유도
- 데이터가 너무 적으면 퀘스트 화면에서 수동 완료 대신 재시도 버튼 제공

## 구현 체크리스트

1. `GET /api/quests/today` 응답에서 `verificationWindow.startTime` 보관
2. 완료 버튼 클릭 시 `startTime=verificationWindow.startTime`, `endTime=now`로 Health Connect 조회
3. 최소 `ExerciseSession`, `Steps`, `Distance`, `ActiveCaloriesBurned`, `HeartRate` 조회
4. 조회 결과를 `healthSamples` 배열로 변환
5. `PATCH /api/quests/{questId}/complete` 호출
6. `200`이면 완료 UI와 보상 UI 표시
7. `409 INSUFFICIENT_HEALTH_PROOF`이면 Health Connect 동기화 대기/재시도 안내

## 참고

기존 수동 완료 요청은 아직 호환된다.

```json
{
  "progressValue": 1,
  "proof": {
    "source": "manual"
  }
}
```

다만 헬스 기반 완료 플로우에서는 `healthSamples`를 보내는 것을 기준으로 구현한다.
