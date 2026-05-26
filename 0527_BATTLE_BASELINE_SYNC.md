# 0527 배틀 건강 데이터 baseline 연동 가이드

## 목표

배틀 점수는 배틀 시작 시점의 누적 건강 데이터 baseline을 저장하고, 이후 최신 동기화 값에서 baseline을 차감해서 계산한다.

```text
배틀 반영 활동량 = 현재 동기화된 활동량 - 배틀 시작 baseline
```

이 방식은 Health Connect 원본 record를 배틀 시간으로 직접 잘라 저장하지 않고, 기존 `health_daily_summaries`와 `battle_participants` baseline 컬럼을 사용한다.

## 백엔드 동작

- 배틀 생성 시 `battle_participants`에 사용자별 baseline을 저장한다.
- 배틀 상세/결과 점수는 현재 건강 데이터 요약에서 baseline을 뺀 값으로 계산한다.
- 배틀 응답에 `healthSync` 메타를 내려준다.
- 배틀 참가자 응답에는 각 참가자의 마지막 건강 데이터 반영 시각인 `latestHealthSyncedAt`을 내려준다.

```json
{
  "healthSync": {
    "required": true,
    "recommended": true,
    "latestSyncedAt": "2026-05-27T01:00:00Z",
    "windowStart": "2026-05-27T01:10:00Z",
    "windowEnd": "2026-05-27T01:40:00Z",
    "staleAfterSeconds": 1800
  }
}
```

참가자 예시:

```json
{
  "participants": [
    {
      "userId": 1,
      "nickname": "나",
      "profileImageUrl": "http://100.89.171.113:8000/media/profile/me.png",
      "me": true,
      "score": 320,
      "result": "PENDING",
      "latestHealthSyncedAt": "2026-05-27T01:20:00Z"
    },
    {
      "userId": 2,
      "nickname": "상대",
      "profileImageUrl": null,
      "me": false,
      "score": 280,
      "result": "PENDING",
      "latestHealthSyncedAt": "2026-05-27T01:05:00Z"
    }
  ]
}
```

필드 의미:

- `required`: 배틀 구간에 반영할 건강 데이터 동기화가 아직 없거나, 마지막 동기화가 배틀 시작 전이다.
- `recommended`: 마지막 동기화가 없거나, 배틀 구간 기준으로 30분 이상 오래되어 최신화가 권장된다.
- `latestSyncedAt`: 백엔드가 알고 있는 마지막 건강 데이터 동기화 시각이다.
- `windowStart`: 배틀 baseline 기준 시각이다.
- `windowEnd`: 현재 기준으로 점수에 반영할 수 있는 구간 끝이다. 종료된 배틀이면 `endsAt`이다.
- `staleAfterSeconds`: 프론트가 stale 판단에 맞출 수 있는 기준값이다. 현재 1800초다.
- `participants[].latestHealthSyncedAt`: 해당 참가자의 건강 데이터가 서버에 마지막으로 반영된 시각이다. 값이 없으면 아직 서버에 반영된 건강 데이터가 없다는 뜻이다.

## 프론트 수정 필요사항

### 1. 배틀 매칭 API 호출 직전 force sync

현재 `BattleMatchingScreen`은 `syncHealthDataWithServerIfStale()`를 호출한다. 이걸 매칭 API 호출 직전에는 강제 동기화로 바꿔야 한다.

```ts
await syncHealthDataWithServer({ force: true });
await matchBattle(durationToBattleMode(duration));
```

이유:

- 백엔드는 매칭 성공 시점의 서버 DB 값을 baseline으로 저장한다.
- 매칭 직전 force sync가 없으면 baseline이 오래된 건강 데이터 기준으로 잡힐 수 있다.

### 2. 배틀 상세 조회 전 sync

`BattleScreen` 진입 전 또는 상세 조회 직전에 건강 데이터를 동기화해야 한다.

권장 흐름:

```ts
await syncHealthDataWithServerIfStale();
const battle = await getBattleDetail(battleId);

if (battle.healthSync?.recommended) {
  await syncHealthDataWithServer({ force: true });
  const refreshedBattle = await getBattleDetail(battleId);
}
```

### 3. 배틀 결과 조회 전 force sync

결과 조회 직전에는 반드시 강제 동기화하는 쪽이 맞다.

```ts
await syncHealthDataWithServer({ force: true });
const result = await getBattleResult(battleId);
```

이유:

- 결과 조회 시점에 백엔드가 배틀을 확정할 수 있다.
- 확정 전에 최신 건강 데이터가 들어와야 최종 점수가 맞게 계산된다.

### 4. 상대 현황 표시

내 데이터는 앱에서 직접 동기화할 수 있지만, 상대 데이터는 상대가 마지막으로 동기화한 시각까지만 반영된다.

프론트에서는 `participants[].latestHealthSyncedAt`을 보고 다음처럼 처리하면 된다.

- 상대 `latestHealthSyncedAt`이 최근이면 일반 점수처럼 표시한다.
- 상대 `latestHealthSyncedAt`이 오래됐으면 "상대 기록 업데이트 대기 중" 또는 "최근 반영 시각"을 표시한다.
- 내 `healthSync.recommended`가 true면 내 데이터는 force sync 후 다시 상세/결과를 조회한다.

### 5. 모바일 타입 추가

`BattleDetail`, `BattleResultDetail`에 아래 타입을 추가하면 된다.

```ts
export interface BattleHealthSync {
  required: boolean;
  recommended: boolean;
  latestSyncedAt: string | null;
  windowStart: string;
  windowEnd: string;
  staleAfterSeconds: number;
}
```

참가자 타입에는 아래 필드를 추가한다.

```ts
latestHealthSyncedAt: string | null;
```

그리고 배틀 상세/결과 응답 타입에 추가한다.

```ts
healthSync: BattleHealthSync;
```

## 주의사항

- 이 구현은 "Health Connect에서 배틀 시간대 record만 직접 잘라 저장"하는 방식이 아니다.
- 서비스 로직상 결과는 "배틀 시작 이후 증가분"으로 계산된다.
- 더 정밀한 원본 검증이 필요하면 별도 `battle_health_samples` 또는 `battle_health_snapshots` 테이블을 추가해야 한다.
