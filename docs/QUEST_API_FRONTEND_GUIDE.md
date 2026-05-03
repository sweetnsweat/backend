# 퀘스트 API 프론트/AI 연동 가이드

## 목적

오늘 사용자가 수행할 퀘스트를 조회한다.

프론트나 AI 서버는 퀘스트 생성 API를 따로 호출하지 않는다. `GET /api/quests/today`를 호출하면 백엔드가 오늘 퀘스트 존재 여부를 확인하고, 없으면 활성 루틴과 오늘 컨디션을 기준으로 자동 생성한다.

## 사용 테이블

새 테이블을 만들지 않고 기존 테이블을 재사용한다.

- `quest_templates`: 반복 사용 가능한 퀘스트 템플릿 저장용. 현재 자동 생성 퀘스트는 템플릿 없이 생성 가능하다.
- `user_quests`: 사용자에게 실제로 발급된 퀘스트 저장용. 오늘 퀘스트 중복 생성을 막기 위해 `user_id + quest_date` 유니크 제약을 사용한다.

이번 작업에서 `user_quests`에 추가한 컬럼은 다음과 같다.

- `quest_type`: `routine`, `off_day`, `recovery`
- `target_metric`: `exercises`, `minutes`
- `source_session_id`: 루틴 세션 기반 퀘스트일 때 참조하는 `routine_sessions.id`
- `condition_adjusted`: 컨디션 때문에 목표가 조정되었는지 여부
- `quest_context_json`: 퀘스트 생성 당시 운동 목록 스냅샷

## 오늘 퀘스트 조회

```http
GET /api/quests/today
Authorization: Bearer {accessToken}
```

동작 흐름:

1. KST 기준 오늘 날짜를 계산한다.
2. 지난 날짜의 미완료 퀘스트를 `EXPIRED` 처리한다.
3. 오늘 퀘스트가 이미 있으면 그대로 반환한다.
4. 오늘 퀘스트가 없으면 온보딩, 오늘 컨디션, 활성 루틴을 확인한다.
5. 오늘 요일에 맞는 루틴 세션이 있으면 `ROUTINE` 퀘스트를 생성한다.
6. 오늘 요일 세션이 없으면 `OFF_DAY` 퀘스트를 생성한다.
7. 컨디션 배율이 낮으면 `RECOVERY` 퀘스트로 대체하거나 목표량을 줄인다.

같은 날 여러 번 호출해도 같은 퀘스트가 반환된다.

## 응답 예시

```json
{
  "success": true,
  "code": "OK",
  "message": "오늘 퀘스트를 조회했습니다.",
  "timestamp": "2026-05-01T18:42:00.000+09:00",
  "data": {
    "id": 12,
    "questDate": "2026-05-01",
    "questType": "ROUTINE",
    "targetMetric": "EXERCISES",
    "status": "ISSUED",
    "completed": false,
    "title": "상체 머신 운동 2개 완료",
    "description": "상체 머신 세션에서 제시된 운동 2개를 완료해 주세요.",
    "targetValue": 2,
    "progressValue": 0,
    "conditionAdjusted": false,
    "routineId": 1,
    "routineName": "초급 헬스장 근력 입문 루틴",
    "sourceSessionId": 3,
    "sessionName": "상체 머신",
    "sessionType": "upper_body",
    "conditionScore": 72.92,
    "exerciseMultiplier": 1.00,
    "rewardCurrency": 30,
    "rewardExp": 20,
    "completedAt": null,
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
}
```

## 퀘스트 타입

- `ROUTINE`: 오늘 요일에 활성 루틴 세션이 있어서 해당 세션 운동 일부를 수행하는 퀘스트
- `OFF_DAY`: 오늘 요일에 루틴 세션이 없어서 걷기/스트레칭 같은 회복성 활동을 수행하는 퀘스트
- `RECOVERY`: 오늘 컨디션이 낮아서 루틴 대신 회복 운동으로 대체된 퀘스트

## 상태값

- `ISSUED`: 오늘 사용자에게 발급된 퀘스트
- `COMPLETED`: 사용자가 완료한 퀘스트
- `EXPIRED`: 날짜가 지나 미완료 처리된 퀘스트

현재 API는 같은 날 완료 후 다시 조회해도 새 퀘스트를 만들지 않고 `COMPLETED` 상태의 기존 퀘스트를 반환한다.

## 퀘스트 완료

```http
PATCH /api/quests/{questId}/complete
Authorization: Bearer {accessToken}
Content-Type: application/json
```

요청 body는 생략 가능하다.

```json
{
  "progressValue": 2,
  "proof": {
    "source": "manual"
  }
}
```

완료 후에는 `status`가 `COMPLETED`, `completed`가 `true`로 내려간다.

## 예외 응답

온보딩이 끝나지 않은 경우:

```json
{
  "code": "ONBOARDING_REQUIRED",
  "detail": "퀘스트를 생성하려면 온보딩 프로필을 먼저 저장해 주세요."
}
```

오늘 컨디션 입력이 없는 경우:

```json
{
  "code": "CONDITION_REQUIRED",
  "detail": "오늘 퀘스트를 생성하려면 먼저 오늘 컨디션을 입력해 주세요."
}
```

활성 루틴이 없는 경우:

```json
{
  "code": "ACTIVE_ROUTINE_REQUIRED",
  "detail": "오늘 퀘스트를 생성하려면 추천 루틴을 선택하거나 내 루틴을 먼저 만들어 주세요."
}
```

지난 날짜의 미완료 퀘스트를 완료하려는 경우:

```json
{
  "code": "QUEST_EXPIRED",
  "detail": "지난 날짜의 미완료 퀘스트는 완료할 수 없습니다."
}
```

## AI 서버 연동 방식

AI 서버는 DB를 직접 조회하지 않고 백엔드 API를 호출하는 것을 권장한다.

스토리 진행 중 퀘스트 분기를 만나면:

```text
AI 서버 -> 백엔드 GET /api/quests/today
백엔드 -> 오늘 퀘스트 반환
AI 서버 -> title, description, exercises를 세계관 문장으로 래핑
AI 서버 -> 프론트에 스토리 응답
```

퀘스트 원본 데이터와 중복 방지는 백엔드가 담당하고, AI 서버는 표현과 세계관 래핑만 담당한다.
