# 2026-05-24 프론트 연동 가이드

이 문서는 2026-05-24 기준 배틀 매칭 구조 변경사항을 정리한다.

- 배틀 매칭은 이제 active 유저 자동 선택이 아니라 `battle_match_queue` 기반 대기열 매칭이다.
- 배틀에 참가하지 않은 유저는 상대방으로 잡히지 않는다.
- 기존 `POST /api/battles/match` 엔드포인트는 유지한다.
- 프론트는 응답의 `data.matchStatus`로 `WAITING` / `MATCHED`를 분기하면 된다.

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

## 6. 배틀 점수 표시 관련

퀘스트 완료 방식에 따라 배틀 점수 반영 여부가 다르다.

| completionType | battleEligible | 배틀 점수 반영 | 완료 퀘스트 개수 표시 |
| --- | --- | --- | --- |
| `VERIFIED` | `true` | O | O |
| `MANUAL` | `false` | X | O |

따라서 사용자가 수동 완료한 퀘스트는 배틀 점수, 운동 시간, 걸음 수, 칼로리에는 반영되지 않을 수 있다. 다만 배틀 화면의 `COMPLETED_QUESTS` 지표에는 완료 개수로 표시된다.

---

## 7. 개발 서버 반영 상태

- 개발 서버 DB에 `battle_match_queue` 테이블 생성 완료
- 개발 서버 백엔드 컨테이너 재배포 완료
- 실제 API 검증 완료
  - 첫 계정 호출: `message=Battle queued`, `matchStatus=WAITING`, `battleId=null`
  - 두 번째 계정 호출: `message=Battle matched`, `matchStatus=MATCHED`, `battleId` 생성

