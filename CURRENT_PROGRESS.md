# 현재 개발 진행 현황

작성일: 2026-04-26

## 요약

현재 백엔드는 로그인 기반 사용자 흐름, 온보딩 프로필 저장, 운동 데이터/기본 루틴 조회, 사용자 활성 루틴 선택, 당일 컨디션 입력/조회까지 구현된 상태다.

중간 발표 시나리오 기준으로는 아래 흐름까지 API로 연결됐다.

```text
로그인
-> 내 정보 조회
-> 온보딩 프로필 저장
-> 기본 루틴 목록 조회
-> 루틴 상세 조회
-> 사용자 활성 루틴 설정
-> 사용자 활성 루틴 조회
-> 당일 컨디션 입력
-> 당일 컨디션 조회
```

아직 퀘스트 생성, 스토리 진행, 보상 지급은 미구현 상태다.

## 배포/커밋 상태

- `main`에는 이전 작업 일부만 배포되어 있다.
- 현재 루틴 조회 API, 온보딩 API, 활성 루틴 API, 컨디션 API 관련 코드는 로컬 작업 트리에 있으며 아직 커밋/푸시하지 않았다.
- 로컬 테스트는 통과했다.

검증 명령:

```bash
./gradlew test
```

결과:

```text
BUILD SUCCESSFUL
```

## 완료된 기능

### 인증/공통

- JWT access token + refresh token 기반 로그인 흐름이 구현되어 있다.
- 공통 성공 응답 `ApiResponse`가 적용되어 있다.
- 전역 예외 처리가 적용되어 있다.
- Swagger/OpenAPI 설정이 있다.
- `/actuator/health`가 인증 없이 접근 가능하도록 배포 반영됐다.
- 개발 로그 확인용 `/dev/logs` API가 배포 반영됐다.

### 사용자

- `GET /api/users/me`
  - 현재 로그인한 사용자 정보를 조회한다.
  - 온보딩 입력값과 푸시 설정값까지 응답한다.

- `PUT /api/users/me/onboarding-profile`
  - 최초 로그인 이후 온보딩 데이터를 저장한다.
  - 현재 입력 항목은 성별, 생년월일, 키, 몸무게, 운동 경험 수준, 선호 운동 유형이다.

- `PUT /api/users/me/routines/active`
  - 사용자가 선택한 루틴을 활성 루틴으로 저장한다.
  - 요청 예시는 아래와 같다.

```json
{
  "routineId": 1
}
```

- `GET /api/users/me/routines/active`
  - 사용자가 설정한 활성 루틴을 상세 조회한다.
  - 활성 루틴이 없으면 `ACTIVE_ROUTINE_NOT_SET`으로 404를 반환한다.

### 운동/루틴

- `exercises`, `routines`, `routine_items` 엔티티가 추가됐다.
- `GET /api/routines/default`
  - 기본 루틴 목록을 조회한다.

- `GET /api/routines/{routineId}`
  - 루틴 상세와 루틴 아이템, 운동 상세 정보를 함께 조회한다.

### 컨디션

- `PUT /api/conditions/today`
  - 오늘 날짜 기준으로 컨디션을 생성하거나 수정한다.
  - 입력 항목은 수면 점수, 스트레스 점수, 피로도 점수, 메모다.
  - 수면 점수는 높을수록 좋고, 스트레스/피로도 점수는 낮을수록 좋게 계산한다.

- `GET /api/conditions/today`
  - 오늘 날짜의 컨디션 로그를 조회한다.
  - 오늘 입력값이 없으면 `CONDITION_NOT_FOUND`로 404를 반환한다.

- 컨디션 점수 계산 규칙:

```text
conditionScore = average(sleepScore, 6 - stressScore, 6 - fatigueScore) * 20
```

- 운동 배율 계산 규칙:

```text
conditionScore < 45: 0.70
conditionScore < 65: 0.85
conditionScore < 80: 1.00
conditionScore >= 80: 1.15
```

### 운동 데이터

- `free-exercise-db`의 운동 데이터 873개를 로컬 DB와 개발 서버 DB에 임포트했다.
- `exercises` 테이블에는 외부 운동 ID, 난이도, 장비, 주요 근육, 보조 근육, 설명, 이미지 URL, 원본 JSON 등을 저장한다.
- 운동 카테고리 분포는 대략 아래와 같다.

```text
strength: 581
stretching: 123
plyometrics: 61
powerlifting: 38
olympic weightlifting: 35
strongman: 21
cardio: 14
```

### 기본 루틴 seed

로컬 DB와 개발 서버 DB에 기본 루틴 3개를 넣어둔 상태다.

- `초급 전신 루틴`
- `유산소 스타터 루틴`
- `회복 스트레칭 루틴`

각 루틴에는 5개 운동 아이템이 연결되어 있다.

## DB 반영 상태

### 로컬 DB

- Docker 컨테이너: `postgres-db`
- DB: `postgres`
- 주요 반영 사항:
  - 사용자 온보딩 컬럼 추가
  - 운동 데이터 컬럼 보강
  - 운동 데이터 873개 임포트
  - 기본 루틴 3개 및 루틴 아이템 15개 seed
  - `users.active_routine_id` 컬럼 및 FK 추가
  - `condition_logs` 컬럼 보강 및 컨디션 점수/운동 배율 컬럼 추가

### 개발 서버 DB

- Tailscale 서버: `100.89.171.113`
- Docker 컨테이너: `postgres-db`
- DB: `postgres`
- 로컬 DB와 동일하게 아래 내용이 반영되어 있다.
  - 사용자 온보딩 컬럼
  - 운동 데이터 컬럼
  - 운동 데이터 873개
  - 기본 루틴 3개
  - 루틴 아이템 15개
  - `users.active_routine_id` 컬럼 및 FK
  - `condition_logs` 컬럼 보강 및 컨디션 점수/운동 배율 컬럼

## 테스트 데이터

로그인 테스트용 사용자 레코드를 로컬 DB와 개발 서버 DB에 넣어뒀다.

```text
loginId: demoUser
password: password123
nickname: Demo User
```

개발 서버에서 로그인 테스트가 성공한 상태다.

## 현재 로컬 코드 변경 범위

아직 커밋되지 않은 주요 추가/수정 파일은 아래와 같다.

- `src/main/java/com/capstone/backend/user/controller/UserController.java`
- `src/main/java/com/capstone/backend/user/service/UserService.java`
- `src/main/java/com/capstone/backend/user/dto/OnboardingProfileRequest.java`
- `src/main/java/com/capstone/backend/user/dto/UpdateActiveRoutineRequest.java`
- `src/main/java/com/capstone/backend/user/entity/User.java`
- `src/main/java/com/capstone/backend/routine/controller/RoutineController.java`
- `src/main/java/com/capstone/backend/routine/service/RoutineService.java`
- `src/main/java/com/capstone/backend/routine/repository/RoutineRepository.java`
- `src/main/java/com/capstone/backend/routine/entity/Exercise.java`
- `src/main/java/com/capstone/backend/routine/entity/Routine.java`
- `src/main/java/com/capstone/backend/routine/entity/RoutineItem.java`
- `src/main/java/com/capstone/backend/routine/dto/RoutineSummaryResponse.java`
- `src/main/java/com/capstone/backend/routine/dto/RoutineDetailResponse.java`
- `src/main/java/com/capstone/backend/condition/controller/ConditionController.java`
- `src/main/java/com/capstone/backend/condition/service/ConditionService.java`
- `src/main/java/com/capstone/backend/condition/repository/ConditionLogRepository.java`
- `src/main/java/com/capstone/backend/condition/entity/ConditionLog.java`
- `src/main/java/com/capstone/backend/condition/dto/ConditionTodayRequest.java`
- `src/main/java/com/capstone/backend/condition/dto/ConditionLogResponse.java`
- `src/test/java/com/capstone/backend/user/controller/UserControllerTest.java`
- `src/test/java/com/capstone/backend/routine/controller/RoutineControllerTest.java`
- `src/test/java/com/capstone/backend/condition/controller/ConditionControllerTest.java`

## 구현된 API 목록

### 인증

- `POST /api/auth/signup`
- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `POST /api/auth/logout`

### 사용자

- `GET /api/users/me`
- `PUT /api/users/me/onboarding-profile`
- `GET /api/users/me/routines/active`
- `PUT /api/users/me/routines/active`

### 루틴

- `GET /api/routines/default`
- `GET /api/routines/{routineId}`

### 컨디션

- `GET /api/conditions/today`
- `PUT /api/conditions/today`

### 개발/운영 확인

- `GET /actuator/health`
- `GET /dev/logs?lines=300`

## 다음 작업 후보

중간 발표 흐름 기준 다음 우선순위는 당일 퀘스트 생성/조회 API다.

1. `quest_templates` 엔티티/테이블 매핑
2. `user_quests` 엔티티/테이블 매핑
3. 발표용 퀘스트 템플릿 seed 정리
4. `GET /api/quests/today` 구현
5. `POST /api/quests/today/generate` 구현
6. 활성 루틴과 오늘 컨디션 배율을 반영해 목표값 산정

그 다음은 스토리 진행과 퀘스트 맥락 연결 순서로 이어가는 것이 적절하다.

## 주의 사항

- 현재는 Flyway/Liquibase 같은 마이그레이션 도구 없이 DB에 직접 DDL과 seed SQL을 반영했다.
- 발표 전에는 현재 DB 변경사항을 재현 가능한 SQL 또는 migration 파일로 정리하는 것이 필요하다.
- 활성 루틴 API는 현재 기존 활성 루틴을 선택하는 단계까지만 구현되어 있다.
- 사용자 커스텀 루틴 생성/수정/삭제는 아직 구현하지 않았다.
