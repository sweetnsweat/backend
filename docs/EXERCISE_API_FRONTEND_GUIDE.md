# 운동 목록/즐겨찾기 API

운동 목록 화면은 `scope`, 종목, 강도, 검색어를 기준으로 운동을 조회하고, 로그인 사용자의 즐겨찾기 여부를 카드별 `liked`로 표시한다.

## 운동 목록 조회

`GET /api/exercises`

인증: `Authorization: Bearer {accessToken}`

쿼리:

| 이름 | 필수 | 예시 | 설명 |
| --- | --- | --- | --- |
| `scope` | X | `all` | `all`, `favorite`, `recent` |
| `category` | X | `수영` | 종목 필터. 전체면 생략하거나 `all` |
| `level` | X | `초급` | 강도/난이도 필터. 전체면 생략하거나 `all` |
| `keyword` | X | `요가` | 운동 이름 검색 |
| `page` | X | `0` | 기본값 0 |
| `size` | X | `50` | 기본값 50, 최대 100 |

`recent`는 최근 운동 기록 저장 기능이 아직 없어서 현재는 빈 목록을 반환한다.

응답 예시:

```json
{
  "success": true,
  "code": "OK",
  "message": "Request succeeded",
  "timestamp": "2026-05-01T17:55:00+09:00",
  "data": {
    "scope": "all",
    "category": null,
    "level": null,
    "keyword": null,
    "page": 0,
    "size": 50,
    "totalCount": 16,
    "totalPages": 1,
    "first": true,
    "last": true,
    "groups": [
      {
        "category": "수영",
        "categoryDisplayName": "수영",
        "count": 3,
        "exercises": [
          {
            "id": 875,
            "name": "자유형 보통 랩 수영",
            "category": "수영",
            "categoryDisplayName": "수영",
            "level": "중급",
            "levelDisplayName": "중급",
            "equipment": "수영장",
            "met": 5.8,
            "estimatedKcalPerHour": 406,
            "primaryMuscles": [],
            "emoji": "🏊",
            "imageUrl": null,
            "liked": true
          }
        ]
      }
    ]
  }
}
```

화면 매핑:

```text
MY 탭 -> scope=all
즐겨찾기 탭 -> GET /api/users/me/exercises/favorites 권장
최근 한 운동 탭 -> scope=recent
종목 필터 -> category
강도 필터 -> level
검색창 -> keyword
하트 상태 -> liked
```

호환 경로로 `GET /api/exercises?scope=favorite`도 사용할 수 있지만, 프론트 즐겨찾기 화면에서는 의미가 명확한 `GET /api/users/me/exercises/favorites`를 우선 사용한다.

필터 alias:

```text
category=헬스 -> DB category=근력
level=입문 -> DB level=초급
level=beginner -> DB level=초급
level=intermediate -> DB level=중급
level=advanced -> DB level=고급
```

## 운동 상세 조회

`GET /api/exercises/{exerciseId}`

카드보다 상세한 설명, 이미지, 근육 정보, 출처까지 포함한다.

`estimatedKcalPerHour` 계산 기준:

```text
1순위: 로그인 사용자의 weightKg
2순위: weightKg가 없으면 gender 기반 fallback
  - male: 75kg
  - female: 60kg
  - prefer_not_to_say: 65kg
3순위: gender도 없으면 70kg

estimatedKcalPerHour = MET * 적용 체중(kg)
```

## 운동 즐겨찾기 설정

`PUT /api/users/me/exercises/{exerciseId}/favorite`

요청:

```json
{
  "liked": true
}
```

해제:

```json
{
  "liked": false
}
```

응답:

```json
{
  "success": true,
  "code": "OK",
  "message": "운동이 즐겨찾기에 추가되었습니다.",
  "data": {
    "exerciseId": 875,
    "liked": true
  }
}
```

## 내 즐겨찾기 운동 목록 조회

`GET /api/users/me/exercises/favorites`

로그인 사용자가 하트로 저장한 운동만 조회한다. 운동 목록과 같은 카드/그룹 응답을 사용하고, `scope`는 항상 `favorite`로 내려간다.

쿼리:

| 이름 | 필수 | 예시 | 설명 |
| --- | --- | --- | --- |
| `category` | X | `유산소` | 종목 필터. 전체면 생략하거나 `all` |
| `level` | X | `초급` | 강도/난이도 필터. 전체면 생략하거나 `all` |
| `keyword` | X | `자전거` | 운동 이름 검색 |
| `page` | X | `0` | 기본값 0 |
| `size` | X | `50` | 기본값 50, 최대 100 |

예시:

```text
GET /api/users/me/exercises/favorites?category=유산소
```

응답:

```json
{
  "success": true,
  "code": "OK",
  "message": "Request succeeded",
  "timestamp": "2026-05-04T09:50:00+09:00",
  "data": {
    "scope": "favorite",
    "category": "유산소",
    "level": null,
    "keyword": null,
    "page": 0,
    "size": 50,
    "totalCount": 1,
    "totalPages": 1,
    "first": true,
    "last": true,
    "groups": [
      {
        "category": "유산소",
        "categoryDisplayName": "유산소",
        "count": 1,
        "exercises": [
          {
            "id": 901,
            "name": "실내 자전거",
            "category": "유산소",
            "categoryDisplayName": "유산소",
            "level": "초급",
            "levelDisplayName": "초급",
            "equipment": "실내 자전거",
            "met": 4.0,
            "estimatedKcalPerHour": 280,
            "primaryMuscles": ["전신"],
            "emoji": "🔥",
            "imageUrl": null,
            "liked": true
          }
        ]
      }
    ]
  }
}
```

## 종목 필터 조회

`GET /api/exercises/categories`

DB에 있는 운동 카테고리를 반환한다. `근력`은 화면 표시용으로 `헬스`를 같이 내려준다.

```json
{
  "category": "근력",
  "categoryDisplayName": "헬스"
}
```
