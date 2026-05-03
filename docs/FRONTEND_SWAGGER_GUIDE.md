# Frontend Swagger Guide

프론트엔드에서 백엔드 API를 연동할 때 Swagger 문서 사용 기준을 정리한다.

## 1) 문서 URL

- 로컬 Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- 개발서버 Swagger UI: `http://100.89.171.113:8080/swagger-ui/index.html`
- Springdoc 생성 스펙(YAML): `http://100.89.171.113:8080/v3/api-docs.yaml`
- 공유 정적 스펙(YAML): `http://100.89.171.113:8080/openapi.yaml`

## 2) 어떤 스펙을 기준으로 볼지

- `openapi.yaml`
  - 팀이 맞춰가는 계약(Contract) 중심
  - 화면/기획 기준으로 API를 먼저 맞출 때 사용
- `v3/api-docs.yaml`
  - 현재 서버 코드에서 자동 생성된 실제 구현 중심
  - 디버깅/실동작 확인 시 우선 사용

권장: 현재 구현 여부와 요청/응답 필드는 `v3/api-docs.yaml`을 우선 확인한다. `openapi.yaml`은 정적 계약 파일이므로 실제 서버 코드와 차이가 날 수 있다.

## 3) 인증(Bearer JWT) 사용 방법

Swagger UI 오른쪽 상단 `Authorize` 버튼에 아래 형식으로 입력한다.

```text
Bearer <accessToken>
```

현재 인증 흐름:

1. `POST /api/auth/signup`
2. `POST /api/auth/login` (accessToken/refreshToken 획득)
3. 보호 API 호출 시 `Authorization: Bearer <accessToken>` 헤더 전달
4. `POST /api/auth/logout` 호출 시 현재 액세스 토큰 폐기 + refresh 토큰 revoke

## 4) 응답 포맷 규칙

### 성공 응답

성공(2xx)은 공통 래퍼 `ApiResponse<T>`를 사용한다.

```json
{
  "success": true,
  "code": "OK",
  "message": "Request succeeded",
  "timestamp": "2026-05-02T16:38:06.344187281+09:00",
  "data": {}
}
```

참고:

- 회원가입은 `201` + `code: "CREATED"`
- 로그인/헬스체크/일반 조회는 `200`

### 실패 응답

실패(4xx/5xx)는 `ProblemDetail` 기반으로 내려간다.

```json
{
  "type": "about:blank",
  "title": "Unauthorized",
  "status": 401,
  "detail": "Authentication required",
  "code": "UNAUTHORIZED",
  "path": "/api/auth/logout",
  "timestamp": "2026-05-02T16:38:06.344187281+09:00"
}
```

프론트 에러 핸들링은 최소 아래 키를 기준으로 구현한다.

- `status`
- `code`
- `detail`

## 5) 프론트 구현 권장사항

- Axios/Fetch 공통 클라이언트에서 `Authorization` 헤더 자동 주입
- 401 응답 시 토큰 제거 후 로그인 화면 이동
- 에러 토스트/모달은 `detail` 우선, 없으면 `title` fallback
- 개발 중에는 Network 탭에서 실제 응답이 `ApiResponse`인지 `ProblemDetail`인지 먼저 확인

## 6) 빠른 확인 체크리스트

- `GET /api/health`가 `ApiResponse` 형태로 오는지
- `POST /api/auth/signup`이 `201`인지
- `POST /api/auth/login`이 `accessToken`/`refreshToken`을 반환하는지
- 보호 API 호출 시 Authorization 헤더 누락으로 401이 나는지(정상 동작 확인)
- `POST /api/stories/play` 요청 Body가 문자열이 아니라 `scenario_id`, `user_message`, `choice_id`, `restart` 필드로 보이는지

## 7) 참고 파일

- Swagger 계약 파일: `src/main/resources/static/openapi.yaml`
- 인증 컨트롤러: `src/main/java/com/capstone/backend/auth/controller/AuthController.java`
- 전역 예외 처리: `src/main/java/com/capstone/backend/global/exception/GlobalExceptionHandler.java`
