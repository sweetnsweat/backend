# 2026-05-24 프론트 미반영 API 정리

이 문서는 백엔드에는 구현되어 있지만 현재 모바일 앱 코드에 아직 완전히 붙지 않은 API를 정리한다.

확인 기준:

- 백엔드: `capstone/backend/src/main/java/com/capstone/backend/**/controller/*Controller.java`
- 모바일: `capstone/mobile/src/services`, `capstone/mobile/src/screens`
- 상태 기준일: 2026-05-24

---

## 1. 우선 적용 필요

### 1.1 기록 페이지 통계 API

```http
GET /api/records/stats?period=WEEKLY
Authorization: Bearer {accessToken}
```

상태:

- 백엔드 구현 완료.
- 모바일 `StatisticsScreen`은 아직 더미 데이터 사용 중.
- 모바일 `StatsService`는 현재 `GET /api/users/me/weekly-stats`만 호출한다.

프론트 작업:

- `StatsService`에 `getRecordStats(period)` 추가.
- `period`는 `WEEKLY`, `MONTHLY`, `YEARLY`로 전송.
- `StatisticsScreen`의 더미 데이터 제거.
- 화면 매핑:
  - 라인 차트: `conditionTrend`
  - 상단 카드: `summary`
  - 운동별 효과 막대 차트: `exerciseEffects`
  - 운동 기록 표: `dailyRecords`
  - 인사이트 카드: `insight`

비고:

- 기존 `GET /api/users/me/weekly-stats`는 마이페이지 간단 요약용으로 유지 가능.
- 기록 페이지는 신규 `GET /api/records/stats`를 쓰는 것이 맞다.

---

### 1.2 건강 데이터 동기화 API

```http
POST /api/health-data/sync
Authorization: Bearer {accessToken}
Content-Type: application/json
```

상태:

- 백엔드 구현 완료.
- Health Connect 데이터를 읽는 모바일 코드는 있음.
- 현재 로그인 후 Health Connect 데이터를 읽고 콘솔 로그만 찍는 흐름이 있다.
- 아직 읽은 데이터를 `/api/health-data/sync`로 보내지는 않는다.
- 퀘스트 완료 시 `healthSamples`를 `/api/quests/{questId}/complete`에 보내는 흐름은 붙어 있다.

프론트 작업:

- Health Connect 동기화 결과를 아래 형식으로 서버에 전송.

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
    }
  ]
}
```

연동 타이밍 권장:

- 로그인 직후 1회.
- 앱 홈 진입 시 최신 동기화가 오래됐으면 1회.
- 배틀 진입 전 1회.
- 기록 페이지 진입 전 1회.

비고:

- 이 API가 붙어야 배틀/통계에서 걸음 수, 운동 시간, 이동 거리, 활동 칼로리가 꾸준히 반영된다.
- 퀘스트 완료 검증과 별개로 일별 건강 요약을 쌓는 용도다.

---

### 1.3 상점 패스 아이템 사용 API

```http
POST /api/shop/items/{itemId}/use
Authorization: Bearer {accessToken}
```

상태:

- 백엔드 구현 완료.
- 모바일 `ShopService`에는 `getShopItems`, `purchaseShopItem`, `equipShopItem`만 있다.
- 모바일 `ShopScreen`에서 패스 아이템은 구매만 가능하고 사용 버튼은 없다.

프론트 작업:

- `ShopService`에 `useShopItem(itemId)` 추가.
- 패스 탭에서 `ownedQuantity > 0`이면 `구매` 외에 `사용하기` 버튼 노출.
- 사용 성공 후 `GET /api/shop/items?type=pass` 재조회.

주요 사용 효과:

| 아이템 | 백엔드 효과 |
| --- | --- |
| 경험치 2배권 | 24시간 EXP 2배 효과 활성화 |
| 퀘스트 스킵권 | 오늘 퀘스트를 수동 완료 처리 |
| 기록 방어권 | 배틀 결과 확정 시 최고 기록 방어 |
| 승률하락 방어권 | 다음 배틀 패배 시 DRAW 처리 |
| 배틀 부활권 | 현재는 패배 방어권 성격으로 처리 |

응답에서 볼 필드:

- `data.item.quantity`: 사용 후 남은 수량
- `data.effectType`
- `data.status`
- `data.expiresAt`
- `data.quest`: 퀘스트 스킵권 사용 시 오늘 퀘스트 완료 결과

---

### 1.4 획득 배지 API

```http
GET /api/users/me/badges
Authorization: Bearer {accessToken}
```

```http
POST /api/users/me/badges/sync
Authorization: Bearer {accessToken}
```

상태:

- 백엔드 구현 완료.
- 퀘스트/배틀 기록 기준 자동 지급 로직 구현 완료.
- 모바일 마이페이지 배지는 현재 정적 UI에 가깝고, 실제 API 호출이 없다.

프론트 작업:

- `BadgeService` 또는 `UserService`에 배지 조회 함수 추가.
- 마이페이지 배지 영역을 `GET /api/users/me/badges` 응답으로 렌더링.
- `earned === true`는 획득 배지, `earned === false`는 잠금 배지로 표시.
- `POST /api/users/me/badges/sync`는 일반 화면 진입마다 호출하지 말고, 개발/복구/수동 새로고침 용도로만 사용.

---

## 2. 있으면 좋은 후순위

### 2.1 로그인 사용자 비밀번호 변경 API

```http
PUT /api/auth/password
Authorization: Bearer {accessToken}
Content-Type: application/json
```

상태:

- 백엔드 구현 완료.
- 모바일은 `POST /api/auth/password-reset/request`를 통한 임시 비밀번호 발급만 붙어 있다.
- 마이페이지에서 현재 비밀번호를 입력하고 새 비밀번호로 변경하는 화면/서비스는 없다.

프론트 작업:

- 마이페이지 또는 계정 설정에 비밀번호 변경 폼 추가.
- 현재 비밀번호, 새 비밀번호, 새 비밀번호 확인 입력.
- 임시 비밀번호 발급과 구분해서 노출.

---

### 2.2 테스트 푸시 발송 API

```http
POST /api/notifications/test
Authorization: Bearer {accessToken}
Content-Type: application/json
```

상태:

- 백엔드 구현 완료.
- 모바일은 FCM 토큰 등록/삭제와 알림 클릭 라우팅은 붙어 있다.
- 앱 내부에서 테스트 푸시를 직접 발송하는 버튼은 없다.

프론트 작업:

- 일반 사용자 화면에는 없어도 된다.
- 개발/QA 화면이 필요하면 테스트 발송 버튼 추가.

비고:

- 운영 기능이라기보다는 FCM 연동 확인용 API다.

---

## 3. 이미 모바일에 붙은 API

아래 API들은 모바일 서비스/화면에서 호출 흔적을 확인했다.

### 인증/프로필

- `POST /api/auth/signup`
- `POST /api/auth/login`
- `GET /api/auth/nickname/check`
- `POST /api/auth/find-login-id`
- `POST /api/auth/password-reset/request`
- `POST /api/auth/logout`
- `GET /api/users/me`
- `PUT /api/users/me`
- `PUT /api/users/me/onboarding-profile`

### 루틴/운동

- `GET /api/routines/recommendations`
- `POST /api/routines/{routineId}/activate`
- `GET /api/users/me/routines`
- `GET /api/users/me/routines/active`
- `GET /api/routines/today`
- `GET /api/routines/{routineId}`
- `POST /api/routines/custom`
- `PUT /api/routines/{routineId}`
- `DELETE /api/routines/{routineId}`
- `GET /api/exercises/categories`
- `GET /api/exercises`
- `GET /api/exercises/{exerciseId}`
- `GET /api/users/me/exercises/favorites`
- `PUT /api/users/me/exercises/{exerciseId}/favorite`

### 퀘스트/컨디션

- `GET /api/quests/today`
- `PATCH /api/quests/{questId}/complete`
- `GET /api/conditions/today`
- `PUT /api/conditions/today`

### 상점/배틀/랭킹

- `GET /api/shop/items`
- `POST /api/shop/items/{itemId}/purchase`
- `POST /api/shop/items/{itemId}/equip`
- `GET /api/battles/me/summary`
- `POST /api/battles/match`
- `GET /api/battles/{battleId}`
- `GET /api/battles/{battleId}/result`
- `GET /api/battles/history`
- `GET /api/rankings/weekly-activity`
- `GET /api/worlds/rankings`
- `GET /api/worlds/rankings/full`
- `GET /api/worlds/{scenarioId}/preview`
- `GET /api/home/world-banners`

### FCM/스토리

- `POST /api/push-tokens`
- `DELETE /api/push-tokens/{tokenId}`
- `POST /api/stories/play`
- `GET /api/stories/play/history`
- `GET /api/stories/chats`
- `GET /api/stories/chats/{scenarioId}`

---

## 5. 프론트 작업 권장 순서

1. `GET /api/records/stats`
2. `POST /api/health-data/sync`
3. `POST /api/shop/items/{itemId}/use`
4. `GET /api/users/me/badges`
5. `PUT /api/auth/password`
6. `POST /api/notifications/test`
7. AI 스토리 세분화 API

기록/배틀 품질에 직접 영향을 주는 것은 `records/stats`와 `health-data/sync`다. 상점 패스 아이템은 구매 후 사용 흐름이 막혀 있으므로 그 다음 우선순위로 처리한다.
