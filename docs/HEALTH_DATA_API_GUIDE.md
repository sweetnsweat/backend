# Health Data API Guide

## Goal

Android Health Connect and iOS HealthKit should not leak into quest logic directly.
Mobile clients may send platform-native record names, but the backend normalizes them into common metric names first.

Current common metric names used for quest work:

- `STEPS`
- `HEART_RATE`
- `DISTANCE`
- `EXERCISE_SESSION`
- `TOTAL_CALORIES_BURNED`
- `SLEEP_SESSION`
- `WEIGHT`

## Sync Summary

`POST /api/health-data/sync`

This endpoint validates and normalizes health samples, then returns per-metric summaries. It does not persist samples yet.

```json
{
  "samples": [
    {
      "source": "health_connect",
      "rawRecordType": "Steps",
      "value": 4815,
      "startTime": "2026-05-14T15:00:00Z",
      "endTime": "2026-05-15T14:59:59.999Z",
      "dataOrigin": "com.sec.android.app.shealth"
    },
    {
      "source": "health_connect",
      "rawRecordType": "HeartRate",
      "value": 87,
      "unit": "bpm",
      "startTime": "2026-05-15T08:20:16Z",
      "dataOrigin": "com.sec.android.app.shealth"
    }
  ]
}
```

Response:

```json
{
  "success": true,
  "code": "OK",
  "message": "건강 데이터가 정규화되었습니다.",
  "data": {
    "acceptedSamples": 2,
    "countByType": {
      "STEPS": 1,
      "HEART_RATE": 1
    },
    "summaries": [
      {
        "type": "HEART_RATE",
        "count": 1,
        "total": 87,
        "average": 87.00,
        "max": 87,
        "unit": "bpm",
        "firstStartTime": "2026-05-15T08:20:16Z",
        "lastEndTime": "2026-05-15T08:20:16Z",
        "dataOrigins": ["com.sec.android.app.shealth"]
      },
      {
        "type": "STEPS",
        "count": 1,
        "total": 4815,
        "average": 4815.00,
        "max": 4815,
        "unit": "count",
        "firstStartTime": "2026-05-14T15:00:00Z",
        "lastEndTime": "2026-05-15T14:59:59.999Z",
        "dataOrigins": ["com.sec.android.app.shealth"]
      }
    ]
  }
}
```

## iOS Shape

iOS can use the same endpoint. The backend maps HealthKit identifiers into the same common metric names.

```json
{
  "samples": [
    {
      "source": "healthkit",
      "rawRecordType": "HKQuantityTypeIdentifierStepCount",
      "value": 3200,
      "startTime": "2026-05-15T00:00:00Z",
      "endTime": "2026-05-15T23:59:59Z",
      "dataOrigin": "com.apple.Health"
    }
  ]
}
```

## Quest Completion Hook

`PATCH /api/quests/{questId}/complete` still accepts the existing manual fields:

```json
{
  "progressValue": 2,
  "proof": {
    "source": "manual"
  }
}
```

It can also receive `healthSamples`. For future health-based quests, backend progress is derived from normalized samples for supported metrics such as `STEPS`, `HEART_RATE`, `HEART_RATE_MAX`, and `HEART_RATE_AVG`.

```json
{
  "healthSamples": [
    {
      "source": "health_connect",
      "rawRecordType": "Steps",
      "value": 4815,
      "startTime": "2026-05-14T15:00:00Z",
      "endTime": "2026-05-15T14:59:59.999Z",
      "dataOrigin": "com.sec.android.app.shealth"
    }
  ]
}
```

## Next Step

1. Add `health_samples` or daily aggregate tables for persisted sync results.
2. Extend quest generation so some quests use `targetMetric='steps'`, `heart_rate_max`, or `heart_rate_avg`.
3. Change quest completion to require health evidence for those health-based target metrics.
4. Add replay protection by storing `source + dataOrigin + rawRecordType + startTime + endTime` as an idempotency key.
