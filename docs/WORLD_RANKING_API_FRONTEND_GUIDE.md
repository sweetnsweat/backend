# 세계관 랭킹 API 프론트 가이드

메인 홈의 세계관 랭킹 요약과 `전체보기` 화면에서 사용하는 API다.

## 랭킹 기준

세계관 랭킹 점수는 현재 진행 중인 채팅 수다.

```text
score = story_progress.status = 'IN_PROGRESS'인 distinct user_key 수
```

- `story_play_logs`는 메시지 수라서 랭킹 기준으로 사용하지 않는다.
- UI의 불꽃 아이콘 옆 숫자는 `score`를 표시한다.
- 우측 상단의 `42일` 같은 진행일 표시는 하지 않기로 했으므로 응답에 별도 필드를 두지 않는다.

## 장르 구조

DB에는 두 가지 장르 정보가 있다.

```text
scenarios.genre
```

- 원본 표시 문구다.
- 예: `현대 막장 회귀 복수 드라마`

```text
scenario_genres
```

- 필터용 정규화 테이블이다.
- 예: `현대`, `막장`, `회귀`, `복수`, `드라마`

응답에는 둘 다 내려간다.

```json
{
  "genre": "현대 막장 회귀 복수 드라마",
  "genres": ["현대", "막장", "회귀", "복수", "드라마"]
}
```

프론트 카테고리 칩 필터는 `genres` 기준으로 맞추면 된다.

## 홈 요약 랭킹

메인 홈의 작은 세계관 랭킹 영역에 사용한다.

```http
GET /api/worlds/rankings?limit=5
Authorization: Bearer {accessToken}
```

쿼리:

| 이름 | 필수 | 기본값 | 설명 |
| --- | --- | --- | --- |
| `limit` | X | `5` | 조회할 개수. 1~20 |

응답은 요약형이다.

```json
{
  "success": true,
  "code": "OK",
  "message": "Request succeeded",
  "data": {
    "metric": "ACTIVE_CHAT_COUNT",
    "rankings": [
      {
        "rank": 1,
        "scenarioId": 4,
        "worldTitle": "월하검귀는 다시 웃지 않는다",
        "displayName": "천류하",
        "imageUrl": "/media/assets/character_천류하_8b1e61b5eb.png",
        "score": 9820
      }
    ]
  }
}
```

## 전체보기 랭킹

세계관 랭킹 전체보기 화면에 사용한다. 무한스크롤 기준 기본 `size`는 50개다.

```http
GET /api/worlds/rankings/full?page=0&size=50&genre=로맨스&keyword=수연
Authorization: Bearer {accessToken}
```

쿼리:

| 이름 | 필수 | 기본값 | 설명 |
| --- | --- | --- | --- |
| `page` | X | `0` | 페이지 번호. 0부터 시작 |
| `size` | X | `50` | 페이지 크기. 1~100 |
| `genre` | X | - | 장르 필터. 전체면 생략하거나 `전체`/`all` |
| `keyword` | X | - | 시나리오 제목/요약/장르/대표 캐릭터 이름/칭호/tags 검색 |

정렬:

```text
score desc, scenarioId asc
```

응답:

```json
{
  "success": true,
  "code": "OK",
  "message": "Request succeeded",
  "data": {
    "metric": "ACTIVE_CHAT_COUNT",
    "genre": "로맨스",
    "keyword": "수연",
    "page": 0,
    "size": 50,
    "totalCount": 1,
    "totalPages": 1,
    "first": true,
    "last": true,
    "hasNext": false,
    "nextPage": null,
    "rankings": [
      {
        "rank": 1,
        "scenarioId": 7,
        "scenarioTitle": "황제의 새장에 다시 피는 꽃",
        "worldTitle": "황제의 새장에 다시 피는 꽃",
        "summary": "회귀와 복수를 다루는 로맨스 판타지 세계관",
        "genre": "로맨스 판타지 회귀 복수",
        "genres": ["로맨스", "판타지", "회귀", "복수"],
        "thumbnailUrl": "/media/assets/thumb.png",
        "worldImageUrl": "/media/assets/world.png",
        "playerImageUrl": "/media/assets/player.png",
        "playerDescription": "황궁에 다시 들어온 플레이어",
        "representativeCharacter": {
          "id": 33,
          "name": "세리엔",
          "title": "예언자",
          "type": "main",
          "imageUrl": "/media/assets/character.png",
          "quote": "운명은 다시 쓰일 수 있어요.",
          "tags": ["예언자", "황실마법", "정보통"]
        },
        "displayName": "세리엔",
        "imageUrl": "/media/assets/character.png",
        "backgroundImageUrl": "/media/assets/world.png",
        "score": 7210
      }
    ]
  }
}
```

## 이미지 사용 기준

카드 대표 이미지:

```text
imageUrl = 대표 캐릭터 imageUrl -> thumbnailUrl -> worldImageUrl -> playerImageUrl
```

배경 이미지:

```text
backgroundImageUrl = worldImageUrl -> thumbnailUrl -> 대표 캐릭터 imageUrl -> playerImageUrl
```

프론트는 카드 레이아웃에 따라 `imageUrl`만 써도 되고, 배경형 카드면 `backgroundImageUrl`을 쓰면 된다.
