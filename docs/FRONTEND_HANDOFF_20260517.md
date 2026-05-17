# Frontend Handoff 2026-05-17

## 작업 커밋

```text
38d3f81537fca5d0a0339df1f85003eb27c1226b
feat: update account profile edit request
```

프론트 반영 대상:

- 임시 비밀번호 메일 발송 요청에 `loginId` 추가
- 사용자 정보 수정 요청에 `gender`, `heightCm`, `weightKg` 추가

## 1. 임시 비밀번호 메일 발송

```http
POST /api/auth/password-reset/request
Content-Type: application/json
```

변경 전에는 이메일만 받았지만, 이제 로그인 아이디와 이메일이 같은 계정에 등록된 정보인지 같이 확인한다.

요청:

```json
{
  "loginId": "demoUser",
  "email": "demo@example.com"
}
```

필드:

| field | required | rule | note |
| --- | --- | --- | --- |
| `loginId` | O | 4~50자 | 로그인 아이디 |
| `email` | O | 이메일 형식, 최대 255자 | 가입 이메일 |

성공 응답:

```json
{
  "success": true,
  "code": "OK",
  "message": "임시 비밀번호를 이메일로 발송했습니다.",
  "data": null
}
```

동작:

- `loginId`와 `email`이 일치하는 계정이 있을 때만 성공한다.
- 성공하면 기존 비밀번호는 즉시 임시 비밀번호로 변경된다.
- 기존 refresh token은 폐기된다.
- 메일에는 10자리 임시 비밀번호만 포함된다.
- 재설정 링크나 별도 토큰은 내려주지 않는다.

주요 실패:

| status | code | case |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR` | `loginId` 누락, 이메일 형식 오류 등 요청값 검증 실패 |
| 404 | `USER_NOT_FOUND` | `loginId`와 `email`이 일치하는 계정 없음 |

## 2. 사용자 정보 수정

```http
PUT /api/users/me
Authorization: Bearer {accessToken}
Content-Type: application/json
```

닉네임, 이메일뿐 아니라 성별, 키, 몸무게도 같은 API에서 수정한다. 수정할 필드만 보내면 된다.

요청:

```json
{
  "nickname": "새닉네임",
  "email": "new@example.com",
  "gender": "female",
  "heightCm": 164.5,
  "weightKg": 58.2
}
```

필드:

| field | required | rule | note |
| --- | --- | --- | --- |
| `nickname` | X | 2~50자 | 보낸 경우 닉네임 중복 검사 |
| `email` | X | 이메일 형식, 최대 255자 | 빈 문자열은 수정 안 함 처리 |
| `gender` | X | `male`, `female`, `prefer_not_to_say` | 빈 문자열은 수정 안 함 처리 |
| `heightCm` | X | 50.0~250.0 | cm 단위 |
| `weightKg` | X | 20.0~300.0 | kg 단위 |

성공 응답:

```json
{
  "success": true,
  "code": "OK",
  "message": "사용자 정보가 수정되었습니다.",
  "data": {
    "id": 1,
    "loginId": "demoUser",
    "nickname": "새닉네임",
    "email": "new@example.com",
    "gender": "female",
    "heightCm": 164.5,
    "weightKg": 58.2,
    "profileImageUrl": null,
    "onboardingCompleted": true,
    "requiresOnboarding": false,
    "activeRoutineId": null,
    "routineSetupRequired": true
  }
}
```

주의:

- 요청 바디에 수정할 값이 하나도 없으면 실패한다.
- 휴대폰 번호는 받지 않는다.
- 생년월일, 운동 경험, 운동 목표, 선호 운동 유형은 이 API 수정 범위가 아니다.
- 프로필 이미지는 기존 `PUT /api/users/me/profile`을 사용한다.

주요 실패:

| status | code | case |
| --- | --- | --- |
| 400 | `NO_UPDATE_FIELDS` | 수정할 필드가 하나도 없음 |
| 400 | `VALIDATION_ERROR` | 성별 값 오류, 키/몸무게 범위 오류, 이메일 형식 오류 등 |
| 409 | `NICKNAME_ALREADY_EXISTS` | 이미 사용 중인 닉네임 |
| 409 | `EMAIL_ALREADY_EXISTS` | 이미 등록된 이메일 |
