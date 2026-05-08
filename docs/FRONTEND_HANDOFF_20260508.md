# Frontend Handoff 2026-05-08

## 작업 범위

상점 MVP 기능을 추가했다.

- 상점 아이템 목록 조회
- 아이템 구매
- 상점 목록에서 보유 여부 표시
- 보유한 이미지 아이템을 프로필 이미지로 장착
- 닉네임 단독 변경 API 추가
- 비밀번호 변경 API 추가
- AI 스토리 Swagger 예시 응답을 실제 프론트 사용 필드 기준으로 보강

브랜치:

```text
feature/shop-purchase
```

## DB 반영

추가 SQL:

```text
db/20260503_create_shop_items_inventory.sql
```

생성/보강 테이블:

- `items`: 아이템 마스터
- `user_items`: 사용자 보유 아이템

기존 보상 테이블 재사용:

- `wallets`: 사용자 골드 잔액
- `wallet_transactions`: 골드 원장

구매 거래는 `wallet_transactions.tx_type = 'purchase'`로 저장한다. 금액은 차감 거래라 음수로 저장된다.

```text
amount = -(item.price_currency * quantity)
```

로컬 DB에는 기본 아이템 4개를 넣어둔 상태다.

```text
블루 트레이닝 스킨     skin       100
골드 프로필 프레임     profile     80
스토리 재도전 티켓     ticket      50
회복 응원 배지         consumable  30
```

## 1. 상점 아이템 목록 조회

```http
GET /api/shop/items
Authorization: Bearer {accessToken}
```

타입 필터:

```http
GET /api/shop/items?type=skin
GET /api/shop/items?type=profile
GET /api/shop/items?type=ticket
```

응답 예시:

```json
{
  "success": true,
  "code": "OK",
  "message": "Request succeeded",
  "data": {
    "items": [
      {
        "id": 1,
        "itemType": "skin",
        "name": "블루 트레이닝 스킨",
        "description": "캐릭터 외형을 파란 트레이닝 테마로 변경합니다.",
        "priceCurrency": 100,
        "sellable": true,
        "owned": true,
        "ownedQuantity": 1,
        "purchasable": true,
        "imageUrl": "/media/assets/item_blue_training_skin.png",
        "metadata": {
          "theme": "blue_training"
        }
      }
    ],
    "balanceCurrency": 250
  }
}
```

프론트 표시 기준:

- `owned`: 보유 여부 표시
- `ownedQuantity`: 보유 수량 표시
- `purchasable`: 현재 골드 기준 구매 가능 여부
- `balanceCurrency`: 현재 보유 골드

## 2. 아이템 구매

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

성공 응답:

```json
{
  "success": true,
  "code": "OK",
  "message": "아이템을 구매했습니다.",
  "data": {
    "item": {
      "id": 10,
      "itemId": 1,
      "itemType": "skin",
      "name": "블루 트레이닝 스킨",
      "description": "캐릭터 외형을 파란 트레이닝 테마로 변경합니다.",
      "quantity": 1,
      "imageUrl": "/media/assets/item_blue_training_skin.png",
      "metadata": {
        "theme": "blue_training"
      }
    },
    "balanceCurrency": 150,
    "transaction": {
      "txType": "purchase",
      "amount": -100
    }
  }
}
```

구매 처리:

- `wallets.balance_currency`에서 총 가격 차감
- `user_items.quantity` 증가
- `wallet_transactions`에 구매 원장 저장
- 전체 처리는 하나의 트랜잭션으로 묶임

## 3. 이미지 아이템 장착

```http
POST /api/shop/items/{itemId}/equip
Authorization: Bearer {accessToken}
```

현재 장착 기능은 프로필 이미지 적용 방식이다. 보유한 아이템의 `imageUrl`을 `users.profile_image_url`에 저장한다.

장착 가능 조건:

- 사용자가 해당 아이템을 보유하고 있어야 함
- `user_items.quantity > 0`
- `itemType`이 `profile` 또는 `skin`
- `imageUrl`이 존재해야 함

성공 응답:

```json
{
  "success": true,
  "code": "OK",
  "message": "아이템을 장착했습니다.",
  "data": {
    "item": {
      "id": 10,
      "itemId": 2,
      "itemType": "profile",
      "name": "골드 프로필 프레임",
      "description": "마이페이지 프로필에 골드 프레임을 적용합니다.",
      "quantity": 1,
      "imageUrl": "/media/assets/item_gold_profile_frame.png",
      "metadata": {
        "frame": "gold"
      }
    },
    "profileImageUrl": "/media/assets/item_gold_profile_frame.png"
  }
}
```

장착 후 `GET /api/users/me`, 로그인 응답, 마이페이지 응답의 `profileImageUrl`에 반영된다.

## 4. 닉네임 변경

```http
PUT /api/users/me/nickname
Authorization: Bearer {accessToken}
Content-Type: application/json
```

요청:

```json
{
  "nickname": "새닉네임"
}
```

성공 응답:

```json
{
  "success": true,
  "code": "OK",
  "message": "닉네임이 변경되었습니다.",
  "data": {
    "id": 1,
    "loginId": "demo",
    "nickname": "새닉네임",
    "email": "demo@example.com",
    "profileImageUrl": null,
    "activeRoutineId": null,
    "routineSetupRequired": true
  }
}
```

처리 기준:

- 닉네임만 변경한다.
- 기존 `PUT /api/users/me`와 동일하게 닉네임 정규화와 중복 검사를 수행한다.
- 닉네임은 2~50자여야 한다.
- 중복 닉네임이면 `NICKNAME_ALREADY_EXISTS`로 실패한다.

## 5. 비밀번호 변경

```http
PUT /api/auth/password
Authorization: Bearer {accessToken}
Content-Type: application/json
```

요청:

```json
{
  "currentPassword": "oldPassword123",
  "newPassword": "newPassword123"
}
```

성공 응답:

```json
{
  "success": true,
  "code": "OK",
  "message": "비밀번호가 변경되었습니다.",
  "data": null
}
```

처리 기준:

- 로그인한 사용자만 호출할 수 있다.
- 현재 비밀번호가 맞는지 확인한 뒤 새 비밀번호로 변경한다.
- 새 비밀번호는 회원가입과 동일하게 8~72자여야 한다.
- 변경 성공 시 기존 refresh token은 폐기한다.
- 현재 access token은 즉시 블랙리스트 처리하지 않는다. 프론트는 변경 성공 후 로그인 화면으로 보내는 방식이 가장 단순하다.

현재 비밀번호 불일치:

```json
{
  "code": "INVALID_CURRENT_PASSWORD",
  "detail": "현재 비밀번호가 일치하지 않습니다."
}
```

## 6. AI 스토리 Swagger 응답 예시 개선

Springdoc 생성 Swagger의 AI 스토리 예시 응답을 프론트에서 실제로 사용하는 필드 중심으로 정리했다.

대상:

```http
POST /api/ai/stories/play
GET /api/ai/stories/play/history
```

주요 반영 필드:

- `server_time`
- `phase`
- `unit_index`, `total_units`
- `narration`
- `dialogue[].name`
- `dialogue[].image_url`
- `dialogue[].character_image_url`
- `dialogue[].character_title`
- `dialogue[].representativeCharacterTitle`
- `choices[].choice_id`
- `choices[].choice_text`
- `workout_quest`
- `is_chapter_completed`
- `is_story_completed`

히스토리 응답 예시에도 캐릭터 표시용 필드를 추가했다.

- `name`
- `image_url`
- `character_image_url`
- `character_title`
- `representativeCharacterTitle`
- `character_type`

## 주요 에러

잔액 부족:

```json
{
  "code": "INSUFFICIENT_BALANCE",
  "detail": "골드가 부족합니다."
}
```

아이템 없음:

```json
{
  "code": "ITEM_NOT_FOUND",
  "detail": "구매할 아이템을 찾을 수 없습니다."
}
```

판매 불가:

```json
{
  "code": "ITEM_NOT_SELLABLE",
  "detail": "판매 중인 아이템이 아닙니다."
}
```

보유하지 않은 아이템 장착:

```json
{
  "code": "ITEM_NOT_OWNED",
  "detail": "보유한 아이템만 장착할 수 있습니다."
}
```

장착 불가 타입:

```json
{
  "code": "ITEM_NOT_EQUIPPABLE",
  "detail": "이미지 장착이 가능한 아이템이 아닙니다."
}
```

## 구현 파일

컨트롤러:

```text
src/main/java/com/capstone/backend/shop/controller/ShopController.java
src/main/java/com/capstone/backend/user/controller/UserController.java
src/main/java/com/capstone/backend/auth/controller/AuthController.java
src/main/java/com/capstone/backend/ai/controller/AiStorySwaggerExamples.java
```

서비스:

```text
src/main/java/com/capstone/backend/shop/service/ShopService.java
src/main/java/com/capstone/backend/user/service/UserService.java
src/main/java/com/capstone/backend/auth/service/AuthService.java
```

엔티티:

```text
src/main/java/com/capstone/backend/shop/entity/Item.java
src/main/java/com/capstone/backend/shop/entity/UserItem.java
```

레포지토리:

```text
src/main/java/com/capstone/backend/shop/repository/ItemRepository.java
src/main/java/com/capstone/backend/shop/repository/UserItemRepository.java
```

기존 보상 엔티티 보강:

```text
src/main/java/com/capstone/backend/reward/entity/Wallet.java
src/main/java/com/capstone/backend/reward/entity/WalletTransaction.java
```

사용자 DTO:

```text
src/main/java/com/capstone/backend/user/dto/UpdateNicknameRequest.java
src/main/java/com/capstone/backend/auth/dto/ChangePasswordRequest.java
```

테스트:

```text
src/test/java/com/capstone/backend/shop/controller/ShopControllerTest.java
src/test/java/com/capstone/backend/user/controller/UserControllerTest.java
src/test/java/com/capstone/backend/auth/controller/AuthControllerTest.java
src/test/java/com/capstone/backend/auth/service/AuthServiceTest.java
src/test/java/com/capstone/backend/ai/controller/AiStoryProxyControllerTest.java
```

## 검증

아래 테스트를 통과했다.

```text
./gradlew test --tests com.capstone.backend.shop.controller.ShopControllerTest
./gradlew test --tests com.capstone.backend.user.controller.UserControllerTest
./gradlew test
```

검증한 케이스:

- 상점 목록 조회
- 타입 필터 조회
- 상점 목록에서 `owned`, `ownedQuantity`, `purchasable` 반환
- 아이템 구매 시 지갑 차감, 인벤토리 증가, 구매 원장 기록
- 잔액 부족 시 구매 실패
- 보유한 이미지 아이템 장착 시 `users.profile_image_url` 변경
- 미보유 아이템 장착 실패
- 장착 불가 타입 장착 실패
- 닉네임 단독 변경 성공
- 중복 닉네임 변경 실패
- 빈 닉네임 변경 실패
- 비밀번호 변경 성공
- 현재 비밀번호 불일치 시 변경 실패
- 비밀번호 변경 성공 시 refresh token 폐기
- AI 스토리 Swagger 예시 응답에 `workout_quest`, `character_image_url` 포함

## 아직 남은 범위

이번 작업은 프로필 이미지 장착까지다. 캐릭터별 스킨 장착은 별도 캐릭터 도메인이 필요하다.

캐릭터 스킨 장착을 하려면 다음 항목이 추가로 필요하다.

- `Character`, `UserCharacter` 엔티티/레포지토리
- 캐릭터 보유 여부 조회
- `user_characters.skin_item_id` 업데이트 API
- 스킨 아이템이 특정 캐릭터에 장착 가능한지 검증하는 정책
