# 2026-05-25 오늘 퀘스트 1개 제한 및 스킵권 연동 정리

이 문서는 2026-05-25 기준 오늘 퀘스트 1개 제한, 퀘스트 스킵권 정책, AI 스토리 진행 API에 주입되는 오늘 퀘스트 상태 필드를 정리한다.

- 하루에 발급되는 퀘스트는 1개만 유지한다.
- 오늘 루틴 세션이 있으면 `ROUTINE`, 오늘 루틴 세션이 없으면 `OFF_DAY`, 컨디션이 낮으면 `RECOVERY`가 발급된다.
- 최초로 발급된 오늘 퀘스트를 하기 싫으면 보유한 퀘스트 스킵권으로 넘길 수 있다.
- 스킵권 사용도 오늘 퀘스트를 소진한 것으로 본다.
- 오늘 퀘스트가 완료 또는 스킵된 뒤에는 스토리를 더 진행해도 AI가 추가 퀘스트 분기를 요청하거나 내려주면 안 된다.
- 백엔드는 AI 서버로 스토리 진행 요청을 전달할 때 오늘 퀘스트 상태를 자동 주입한다.

---

## 1. 최종 정책

### 오늘 퀘스트 발급

```text
프론트가 스토리 진행 API 호출
-> 백엔드가 AI 서버 요청에 오늘 퀘스트 상태 주입
-> AI 서버가 퀘스트 분기 필요 여부 판단
-> AI 서버가 백엔드 퀘스트 API를 조회
-> 백엔드가 오늘 퀘스트가 없으면 최초 퀘스트 1개 발급
-> AI 서버가 퀘스트 원본을 세계관 문장으로 래핑
-> 프론트는 AI 스토리 응답 안의 래핑된 퀘스트를 표시
```

프론트는 스토리 중 퀘스트 발급을 위해 `GET /api/quests/today`를 직접 호출하지 않는다. 같은 날짜에는 백엔드가 새 퀘스트를 계속 만들지 않고 기존 오늘 퀘스트를 재사용한다. `GET /api/quests/today?issueRecoveryIfCompleted=true`로 호출해도 오늘 이미 발급된 퀘스트가 있으면 같은 퀘스트만 반환한다.

오늘 신규 발급 기준:

| 조건 | 발급 타입 | 비고 |
| --- | --- | --- |
| 오늘 퀘스트가 이미 있음 | 기존 퀘스트 재사용 | 상태가 `COMPLETED`여도 새로 만들지 않음 |
| 오늘 퀘스트 없음 + 컨디션 낮음 | `RECOVERY` | 회복 스트레칭 10분 |
| 오늘 퀘스트 없음 + 오늘 루틴 세션 있음 | `ROUTINE` | 세션 운동 목록 포함 |
| 오늘 퀘스트 없음 + 오늘 루틴 세션 없음 | `OFF_DAY` | 걷기/스트레칭 회복 퀘스트 |

### 퀘스트 스킵권 사용

```text
오늘 최초 퀘스트가 발급됨
-> 사용자가 운동을 하지 않고 넘기고 싶음
-> 프론트가 AI 응답에 포함된 퀘스트 화면에서 스킵권 사용 버튼 노출
-> 스킵권이 있으면 사용 API 호출
-> 백엔드는 오늘 퀘스트를 completed 상태로 처리
-> 이후 스토리 진행 시 AI에는 today_quest_completed=true 전달
```

스킵권은 “스토리 중간 게이트 통과권”이 아니라 “오늘 최초 퀘스트를 수행하지 않고 소진하는 이용권”으로 본다.
프론트는 보통 AI 응답에 퀘스트가 포함된 화면에서 스킵권 사용 버튼을 노출하면 된다. 단, 백엔드는 아직 오늘 퀘스트가 없는 상태에서 스킵권을 사용해도 내부적으로 오늘 퀘스트를 1개 생성한 뒤 바로 완료 처리한다.

---

## 2. 기존 구현과 달라진 점

이전 논의에서는 스킵권을 스토리 진행 중 추가 퀘스트 게이트를 넘기는 용도로 검토했다.

현재 결정은 다르다.

```text
이전 검토안:
오늘 퀘스트 완료 후 추가 스토리 게이트를 스킵권으로 통과

최종 결정:
오늘 최초 퀘스트 자체를 스킵권으로 완료/소진
```

따라서 기존 `POST /api/shop/items/{itemId}/use` 흐름을 활용할 수 있다.

---

## 3. 실제 호출 흐름

최종 UX 흐름은 아래 순서로 고정한다.

```text
스토리 진행
-> AI가 백엔드 퀘스트 API 조회 후 퀘스트를 래핑해서 내려줌
-> 프론트가 퀘스트 화면 표시
-> 퀘스트 스킵권을 보유 중이면 스킵권 사용 UI 노출
-> 사용자가 스킵권 사용 누름
-> 프론트가 POST /api/shop/items/{itemId}/use 호출
-> 백엔드가 오늘 퀘스트를 완료된 것으로 처리
-> 프론트가 이어서 POST /api/stories/play 호출
-> 백엔드가 AI 요청에 today_quest_completed=true 주입
-> AI는 추가 퀘스트 없이 스토리를 이어감
```

### 3.1 스토리 진행

```http
POST /api/stories/play
Authorization: Bearer {accessToken}
Content-Type: application/json
```

요청 예시:

```json
{
  "scenario_id": 100,
  "user_message": "계속 진행한다."
}
```

백엔드는 이 요청을 AI 서버로 전달하기 전에 오늘 퀘스트 상태를 주입한다.

AI 서버는 `can_issue_today_quest=true`이고 스토리상 퀘스트가 필요한 타이밍이면 백엔드의 퀘스트 API를 조회한다.

```http
GET /api/quests/today/by-user?userId={userId}
```

AI 서버는 이 응답을 그대로 노출하지 않고 세계관 문장으로 래핑해서 프론트에 내려준다.

### 3.2 퀘스트 스킵권 UI 노출

프론트는 AI가 내려준 퀘스트 화면에서 스킵권 사용 버튼을 제공한다. 단, 버튼은 사용자가 퀘스트 스킵권을 보유 중일 때만 노출한다.

스킵권 보유 여부는 패스 목록에서 확인한다.

```http
GET /api/shop/items?type=pass
Authorization: Bearer {accessToken}
```

`퀘스트 스킵권` 아이템의 `ownedQuantity > 0`이면 사용 버튼을 노출할 수 있다. `type=pass`는 `ticket`, `pvp_badge`, `gift`, `consumable` 타입 중 배지가 아닌 아이템을 묶어서 반환한다. 스킵권 사용에는 응답의 `id`를 아래 `{itemId}` 경로 변수로 사용한다.

패스 목록 예시:

```json
{
  "id": 6,
  "itemType": "ticket",
  "itemTypeLabel": "이용권",
  "category": "pass",
  "categoryLabel": "패스",
  "name": "퀘스트 스킵권",
  "owned": true,
  "ownedQuantity": 1,
  "purchasable": true,
  "effect": "퀘스트 스킵 · 1회",
  "imageUrl": "http://100.89.171.113:8000/media/assets/item_quest_skip.png"
}
```

### 3.3 퀘스트 스킵권 사용

사용자가 스킵권 사용을 누르면 프론트는 상점 아이템 사용 API를 호출한다.

```http
POST /api/shop/items/{itemId}/use
Authorization: Bearer {accessToken}
```

응답 예시:

```json
{
  "success": true,
  "code": "OK",
  "message": "아이템을 사용했습니다.",
  "data": {
    "item": {
      "id": 501,
      "itemId": 5,
      "itemType": "ticket",
      "itemTypeLabel": "이용권",
      "name": "퀘스트 스킵권",
      "description": "오늘의 퀘스트를 건너뛸 수 있어요",
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
      "id": 1308,
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

스킵권 완료는 실제 건강 데이터 검증 완료가 아니므로 `battleEligible=false`로 본다.
이미 완료된 오늘 퀘스트에 다시 스킵권을 사용하면 `409 QUEST_ALREADY_COMPLETED`가 내려간다.

### 3.4 스킵 후 스토리 재진행

스킵권 사용 성공 후 프론트는 다시 스토리 진행 API를 호출한다.

```http
POST /api/stories/play
Authorization: Bearer {accessToken}
Content-Type: application/json
```

이때 백엔드가 AI 서버 요청에 `today_quest_completed=true`, `today_quest_skipped=true`, `can_issue_today_quest=false`를 주입하므로 AI 서버는 같은 날 추가 퀘스트를 내려주면 안 된다.

---

## 4. AI 서버로 주입되는 퀘스트 상태

백엔드는 다음 스토리 진행 API들을 AI 서버로 프록시할 때 오늘 퀘스트 상태를 자동으로 붙인다.

- `POST /api/stories/play`

프론트는 이 필드를 직접 보내거나 직접 조회할 필요가 없다. 백엔드가 로그인 사용자 기준으로 조회해서 AI 서버 요청 body에 주입한다.

AI 서버로 전달되는 필드:

```json
{
  "today_quest_completed": true,
  "today_quest_issued": true,
  "today_quest_skipped": true,
  "can_issue_today_quest": false,
  "quest_state": {
    "quest_date": "2026-05-25",
    "daily_quest_limit": 1,
    "today_quest_issued": true,
    "today_quest_completed": true,
    "today_quest_skipped": true,
    "can_issue_today_quest": false,
    "today_quest_id": 1308,
    "today_quest_status": "COMPLETED",
    "today_quest_completion_type": "MANUAL"
  }
}
```

---

## 5. AI 서버 판단 기준

AI 서버는 아래 기준으로 퀘스트 분기 여부를 판단한다. 실제 퀘스트 원본 생성과 중복 방지는 백엔드가 담당한다.

| 필드 | 의미 | AI 처리 |
| --- | --- | --- |
| `can_issue_today_quest=true` | 오늘 아직 퀘스트가 발급되지 않음 | 운동 분기에서 백엔드 오늘 퀘스트 조회 가능 |
| `today_quest_issued=true` | 오늘 퀘스트가 이미 발급됨 | 새 퀘스트 요청 금지, 기존 퀘스트 기준으로 진행 |
| `today_quest_completed=true` | 오늘 퀘스트가 완료 또는 스킵됨 | 이후 스토리에서 퀘스트 분기 생성 금지 |
| `today_quest_skipped=true` | 스킵권으로 오늘 퀘스트를 넘김 | 완료 상태로 보고 추가 퀘스트 분기 생성 금지 |

AI가 가장 우선해서 봐야 하는 값은 `can_issue_today_quest`와 `today_quest_completed`다.

```text
if can_issue_today_quest == true:
    운동 분기에서 백엔드 오늘 퀘스트 조회 API를 호출할 수 있음
else:
    오늘은 더 이상 새 운동 퀘스트 분기를 만들면 안 됨
```

---

## 6. 백엔드 구현 메모

추가된 구성:

- `AiStoryQuestState`
  - AI 서버로 전달할 오늘 퀘스트 상태 DTO
- `AiStoryQuestStateService`
  - `user_quests`에서 KST 기준 오늘 퀘스트 상태 조회
  - 스킵권 사용 여부는 `proof_json.source = "shop_pass"` 또는 `proof_json.submittedProof.source = "shop_pass"`로 판단
- `AiStoryRequestFactory`
  - AI 요청 body에 퀘스트 상태 필드 자동 추가
- `AiStoryProxyController`
  - 스토리 진행 API 프록시 시 `AiStoryQuestStateService`를 통해 상태 주입

검증한 테스트:

```bash
./gradlew test --tests com.capstone.backend.ai.controller.AiStoryProxyControllerTest
./gradlew test --tests com.capstone.backend.quest.controller.QuestControllerTest
```

---

## 7. 프론트/AI 주의사항

- 프론트는 `today_quest_completed`, `quest_state`를 직접 보내지 않는다.
- 프론트는 스토리 중 퀘스트 발급을 위해 `/api/quests/today`를 직접 조회하지 않는다.
- AI 서버가 백엔드 퀘스트 API를 조회하고, 퀘스트 원본을 세계관 문장으로 래핑해서 프론트에 내려준다.
- 프론트는 AI가 내려준 퀘스트 화면에서 오늘 퀘스트가 `completed=false`이고 `퀘스트 스킵권.ownedQuantity > 0`일 때 스킵권 사용 버튼을 노출하면 된다.
- AI 서버는 백엔드가 주입한 `quest_state`를 보고 추가 퀘스트 분기 요청 여부를 결정한다.
- 스킵권 완료는 운동 검증 완료가 아니므로 배틀 점수 반영 대상이 아니다.
- 오늘 퀘스트가 이미 완료 또는 스킵된 경우, 스토리 응답은 일반 대화/선택지 위주로 이어져야 한다.
