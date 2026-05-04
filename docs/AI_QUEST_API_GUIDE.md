# AI 서버용 퀘스트 조회 API

AI 서버가 스토리 진행 중 운동 퀘스트 분기를 만났을 때 백엔드에서 오늘 퀘스트 원본을 조회하기 위한 API다.

이 API는 캡스톤 데모 편의를 위해 `userId`만으로 호출할 수 있게 열어둔 서버 간 연동용 API다.

## Endpoint

```http
GET /api/quests/today/by-user?userId={userId}
```

예시:

```http
GET http://100.89.171.113:8080/api/quests/today/by-user?userId=14
Accept: application/json
```

`Authorization` 헤더는 필요 없다.

## Query

| 이름 | 필수 | 설명 |
| --- | --- | --- |
| `userId` | O | 백엔드 `users.id`. 1 이상이어야 한다. |

`user_id`가 아니라 `userId`로 보낸다.

## 동작

기존 사용자용 API인 `GET /api/quests/today`와 같은 퀘스트 생성/조회 로직을 사용한다.

```text
1. KST 기준 오늘 날짜 계산
2. 지난 미완료 퀘스트 EXPIRED 처리
3. 오늘 퀘스트가 있으면 그대로 반환
4. 없으면 온보딩, 오늘 컨디션, 활성 루틴 확인
5. 오늘 루틴 세션이 있으면 ROUTINE 퀘스트 생성
6. 오늘 루틴 세션이 없으면 OFF_DAY 퀘스트 생성
7. 컨디션이 낮으면 RECOVERY 퀘스트 생성
```

같은 날 여러 번 호출해도 같은 퀘스트가 반환된다.

## 응답

백엔드 공통 `ApiResponse`로 감싸서 내려간다.

```json
{
  "success": true,
  "code": "OK",
  "message": "오늘 퀘스트를 조회했습니다.",
  "timestamp": "2026-05-04T19:00:00+09:00",
  "data": {
    "id": 12,
    "questDate": "2026-05-04",
    "questType": "ROUTINE",
    "targetMetric": "ROUTINE",
    "status": "ISSUED",
    "completed": false,
    "title": "월요일 전신 루틴 완료",
    "description": "월요일 전신 세션의 운동 루틴을 완료해 주세요. 포함된 운동은 총 3개입니다.",
    "targetValue": 1,
    "progressValue": 0,
    "conditionAdjusted": false,
    "routineId": 9,
    "routineName": "데모 수정 가능 루틴",
    "sourceSessionId": 25,
    "sessionName": "월요일 전신 편집 테스트",
    "sessionType": "full_body",
    "sessionTypeDisplayName": "전신",
    "conditionScore": 72.92,
    "exerciseMultiplier": 1.00,
    "rewardCurrency": 30,
    "rewardExp": 20,
    "completedAt": null,
    "exercises": [
      {
        "exerciseId": 1,
        "exerciseName": "3/4 윗몸일으키기",
        "category": "근력",
        "seq": 1,
        "targetSets": 3,
        "targetReps": 12,
        "targetDurationSec": null
      }
    ]
  }
}
```

## AI 서버에서 주로 볼 필드

| 필드 | 용도 |
| --- | --- |
| `data.questType` | `ROUTINE`, `OFF_DAY`, `RECOVERY` 분기 |
| `data.title` | 원본 퀘스트 제목 |
| `data.description` | 원본 퀘스트 설명 |
| `data.sessionName` | 오늘 루틴 세션명 |
| `data.sessionTypeDisplayName` | 화면/문장용 세션 유형 한글명 |
| `data.exercises` | 루틴형 퀘스트일 때 포함 운동 목록 |
| `data.exercises[].exerciseName` | 운동명 |
| `data.exercises[].targetSets` | 목표 세트 수 |
| `data.exercises[].targetReps` | 목표 반복 수 |
| `data.exercises[].targetDurationSec` | 목표 수행 시간 |

AI 서버는 이 원본 데이터를 세계관 문장으로 래핑해서 프론트에 내려주면 된다.

## 에러

온보딩, 컨디션, 활성 루틴이 없으면 기존 퀘스트 API와 같은 에러가 내려간다.

```json
{
  "code": "CONDITION_REQUIRED",
  "detail": "오늘 퀘스트를 생성하려면 먼저 오늘 컨디션을 입력해 주세요."
}
```

대표 에러:

| code | 의미 |
| --- | --- |
| `INVALID_USER_ID` | `userId`가 없거나 1 미만 |
| `USER_NOT_FOUND` | 사용자를 찾을 수 없음 |
| `ONBOARDING_REQUIRED` | 온보딩 미완료 |
| `CONDITION_REQUIRED` | 오늘 컨디션 미입력 |
| `ACTIVE_ROUTINE_REQUIRED` | 활성 루틴 없음 |

## Python 호출 예시

```python
import requests

BACKEND_BASE_URL = "http://100.89.171.113:8080"

def fetch_today_quest(user_id: int) -> dict | None:
    response = requests.get(
        f"{BACKEND_BASE_URL}/api/quests/today/by-user",
        params={"userId": user_id},
        headers={"Accept": "application/json"},
        timeout=2.0,
    )
    if response.status_code == 404:
        return None
    response.raise_for_status()
    return response.json()
```
