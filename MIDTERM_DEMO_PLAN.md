# 중간 발표 개발 우선순위 체크리스트

## 목표 시나리오

중간 발표에서는 아래 흐름이 끊기지 않는 것을 최우선으로 둔다.

```text
로그인
-> 운동 경험 설정
-> 더미 루틴 확인
-> 컨디션 입력
-> 당일 퀘스트 조회/생성
-> 스토리 진행
-> 스토리 맥락이 붙은 퀘스트 제시
-> 퀘스트 완료
-> 보상/완료 상태 확인
```

## 우선순위 원칙

- 발표 시연 흐름에 직접 필요한 기능을 먼저 만든다.
- 운동 루틴, 운동 경험, 퀘스트 템플릿, 스토리 콘텐츠는 V1에서 더미/시드 데이터로 박아둔다.
- OAuth, FCM, 실제 웨어러블 검증, 상점/경쟁은 발표 시나리오에서 제외하고 API 확장 여지만 남긴다.
- 스토리와 퀘스트는 분리하지 않고 발표용으로 연결한다. 컨디션 입력 후 베이스 퀘스트를 만들고, 스토리 진행/분기 후 `scenarioId`, `branchChoiceId`, `questContextJson`을 붙인다.
- DB는 PostgreSQL 기준으로 구현하고, 테스트는 H2 또는 테스트 컨테이너로 분리한다.

## 참고 문서 기준

- `WORKLOG.md`
  - 로그인은 JWT access token + refresh token 기준
  - 컨디션 입력 항목은 수면, 스트레스, 피로도
  - 컨디션 점수는 평균 또는 가중합으로 계산하고 운동 배율로 매핑
  - 발표 핵심 설명은 캐릭터 챗 + 퀘스트 + 층 상승/스토리 진행이 운동 지속 동기를 만든다는 방향
- `DB_DICTIONARY_V1_4.md`
  - PostgreSQL + Spring Boot + JPA
  - `routines`, `user_quests`, `wallet_transactions` 재사용
  - 스토리 데이터는 `story_*`, 런타임은 `user_story_*`
  - `user_quests`에 `scenario_id`, `branch_choice_id`, `quest_context_json` 연계
- UML 시퀀스
  - `SD-0102 로그인`
  - `SD-0206 컨디션 입력`
  - `SD-0302 퀘스트 생성`
  - `SD-0304 퀘스트 완료 처리`
  - `SD-0402 스토리 진행`
  - `SD-0404 스토리 이벤트 발생`

## P0: 발표 시나리오 필수

### 1. 프로젝트/공통 기반

- [ ] 패키지 구조 확정
  - `auth`
  - `user`
  - `routine`
  - `condition`
  - `quest`
  - `story`
  - `reward`
  - `global`
- [ ] 공통 응답 포맷 정의
  - 성공 응답
  - 검증 실패 응답
  - 인증 실패 응답
- [ ] 전역 예외 처리 추가
- [ ] API 요청/응답 DTO 위치 규칙 확정
- [ ] 로컬/개발서버 DB 연결 환경변수 정리
  - `DB_URL`
  - `DB_USERNAME`
  - `DB_PASSWORD`
- [ ] 발표용 seed 데이터 투입 방식 결정
  - 우선순위 1: `CommandLineRunner`
  - 우선순위 2: `data.sql`
  - 운영 전환 시 Flyway/Liquibase 검토

### 2. 인증/사용자 흐름

- [ ] `users` 최소 엔티티 생성
  - `id`
  - `loginId`
  - `passwordHash`
  - `nickname`
  - `exerciseExperience`
  - `preferredExercise`
  - `createdAt`
  - `updatedAt`
- [ ] `refresh_tokens` 최소 엔티티 생성
  - `id`
  - `userId`
  - `token`
  - `expiresAt`
  - `revoked`
- [ ] 발표용 회원가입 API 구현
  - `POST /api/auth/signup`
- [ ] 로그인 API 구현
  - `POST /api/auth/login`
  - access token + refresh token 반환
- [ ] JWT 필터/토큰 서비스 구현
- [ ] 현재 사용자 조회 API 구현
  - `GET /api/users/me`
- [ ] 운동 경험 설정 API 구현
  - `PUT /api/users/me/exercise-profile`
  - 발표 흐름상 로그인 직후 호출
- [ ] 더미 사용자 seed 추가
  - 발표 중 회원가입을 생략해야 할 때 사용

### 3. 운동 루틴 더미 데이터

- [ ] `exercises` 엔티티 생성
- [ ] `routines` 엔티티 생성
- [ ] `routine_items` 엔티티 생성
- [ ] 발표용 운동/루틴 seed 추가
  - 초급 전신 루틴
  - 유산소 루틴
  - 근력 루틴
- [ ] 디폴트 루틴 조회 API 구현
  - `GET /api/routines/default`
- [ ] 루틴 상세 조회 API 구현
  - `GET /api/routines/{routineId}`
- [ ] 사용자 활성 루틴 조회 API 구현
  - `GET /api/users/me/routines/active`
- [ ] 발표 범위에서는 루틴 생성/수정/삭제를 후순위로 둔다.

### 4. 컨디션 입력

- [ ] `condition_logs` 엔티티 생성
  - `userId`
  - `logDate`
  - `conditionLevel`
  - `sleepScore`
  - `stressScore`
  - `fatigueScore`
  - `energyLevel`
  - `conditionScore`
  - `exerciseMultiplier`
- [ ] 컨디션 입력/수정 API 구현
  - `PUT /api/conditions/today`
- [ ] 당일 컨디션 조회 API 구현
  - `GET /api/conditions/today`
- [ ] 컨디션 점수 계산 구현
  - V1은 오늘 컨디션, 수면, 스트레스, 에너지 입력값 기준
- [ ] 운동 배율 매핑 구현
  - 낮음: `0.7x`
  - 보통 이하: `0.85x`
  - 보통: `1.0x`
  - 좋음: `1.15x`
- [ ] 컨디션 저장 후 당일 퀘스트 생성 트리거 연결
  - `SD-0206`, `SD-0302` 기준

### 5. 퀘스트 생성/조회

- [ ] `quest_templates` 엔티티 생성
  - `title`
  - `description`
  - `questType`
  - `targetMetric`
  - `baseTargetValue`
  - `rewardAmount`
  - `verificationRules`
  - `active`
- [ ] `user_quests` 엔티티 생성
  - `userId`
  - `questTemplateId`
  - `questDate`
  - `title`
  - `description`
  - `targetValue`
  - `progressValue`
  - `status`
  - `verificationStatus`
  - `proofJson`
  - `scenarioId`
  - `branchChoiceId`
  - `questContextJson`
- [ ] 발표용 퀘스트 템플릿 seed 추가
  - 걷기/운동 시간/루틴 완료형
- [ ] 당일 퀘스트 조회 API 구현
  - `GET /api/quests/today`
- [ ] 당일 퀘스트 생성 API 구현
  - `POST /api/quests/today/generate`
- [ ] 중복 생성 방지 구현
  - 같은 사용자 + 같은 날짜는 기존 퀘스트 반환
- [ ] 루틴 + 컨디션 기반 목표값 조정 구현
  - 더미 루틴 기준으로 target 산정
  - 컨디션 배율 반영
- [ ] 스토리 맥락 붙이기 서비스 구현
  - `scenarioId`
  - `branchChoiceId`
  - `questContextJson`

### 6. 스토리 진행

- [ ] `story_worlds` 엔티티 생성
- [ ] `story_characters` 엔티티 생성
- [ ] `story_scenarios` 엔티티 생성
- [ ] `story_branch_points` 엔티티 생성
- [ ] `story_branch_options` 엔티티 생성
- [ ] `user_story_progress` 엔티티 생성
- [ ] `user_story_choices` 엔티티 생성
- [ ] `user_story_sessions` 엔티티 생성
- [ ] 발표용 기본 세계관 seed 추가
  - 탑 등반/운동 지속 동기 구조
  - 기본 AI 캐릭터 1명
  - 1일차 시나리오
  - 1개 분기점과 2~3개 선택지
- [ ] 스토리 상태 조회 API 구현
  - `GET /api/stories/progress`
- [ ] 스토리 시작/이어하기 API 구현
  - `POST /api/stories/start`
- [ ] 스토리 입력 처리 API 구현
  - `POST /api/stories/play`
- [ ] 분기 선택 처리 구현
  - 선택지 번호 검증
  - `user_story_choices` 저장
- [ ] 분기 선택 직후 퀘스트 맥락 연결
  - `QuestService` 호출
  - `user_quests` 업데이트
- [ ] V1에서는 AI 응답을 실제 LLM 호출 대신 더미 응답으로 시작
  - 발표 안정성 우선
  - OpenAI 연동은 P1로 분리

### 7. 퀘스트 완료/보상

- [ ] `wallets` 엔티티 생성
- [ ] `wallet_transactions` 엔티티 생성
- [ ] 필요 시 `items`, `user_items` 최소 엔티티 생성
- [ ] 퀘스트 진행 기록 API 구현
  - `PATCH /api/quests/{questId}/progress`
- [ ] 퀘스트 완료 API 구현
  - `POST /api/quests/{questId}/complete`
- [ ] 발표용 검증 로직 구현
  - V1은 proof 입력 또는 수동 완료 허용
  - 실제 웨어러블/헬스 데이터 검증은 P1/P2로 분리
- [ ] 완료 성공 시 상태 업데이트
  - `status=completed`
  - `verificationStatus=passed`
  - `proofJson` 저장
- [ ] 보상 지급 처리 구현
  - wallet 잔액 증가
  - `wallet_transactions.txType='quest_reward'`
- [ ] 완료 응답에 보상 결과 포함

## P1: 발표 품질 상승

- [ ] OpenAI API 연동 래퍼 추가
  - 실패 시 더미 응답 fallback
- [ ] 캐릭터 대화 API 구현
  - `POST /api/characters/{characterId}/chat`
- [ ] 스토리 장면 결과에 캐릭터 대사/감정 필드 추가
- [ ] 마이페이지/컨디션 요약 API 추가
  - `GET /api/users/me/summary`
- [ ] 퀘스트 이력 조회 API 추가
  - `GET /api/quests/history`
- [ ] 발표용 Swagger/OpenAPI 문서 추가
- [ ] 개발서버 배포 스크립트 추가
  - 이미지 빌드
  - 컨테이너 재시작
  - health check

## P2: 발표 이후로 미뤄도 되는 것

- [ ] 카카오 OAuth 로그인
- [ ] FCM 푸시 알림
- [ ] 실제 애플헬스/삼성헬스 연동
- [ ] GPS/웨어러블 기반 자동 퀘스트 검증
- [ ] 루틴 생성/수정/삭제 전체 CRUD
- [ ] 경쟁 시스템
- [ ] 상점/아이템 구매
- [ ] 스토리 결제/해금
- [ ] 관리자용 콘텐츠 관리 UI

## 추천 구현 순서

### 1차 스프린트: API 흐름 뚫기

- [ ] 인증 없이 임시 `userId=1`로 루틴/컨디션/퀘스트/스토리 API 흐름 먼저 연결
- [ ] seed 데이터로 루틴, 퀘스트 템플릿, 기본 세계관 생성
- [ ] 컨디션 입력 시 당일 퀘스트 생성
- [ ] 스토리 분기 선택 시 퀘스트 맥락 업데이트
- [ ] 퀘스트 완료 시 wallet 보상 지급

### 2차 스프린트: 인증 붙이기

- [ ] JWT 로그인 구현
- [ ] `userId=1` 하드코딩 제거
- [ ] 인증 사용자 기준 API로 전환
- [ ] 발표용 더미 계정으로 전체 시나리오 리허설

### 3차 스프린트: 발표 안정화

- [ ] 모든 API를 프론트 시연 순서대로 점검
- [ ] 개발서버 Docker 이미지 재빌드/재배포
- [ ] `GET /actuator/health` 확인
- [ ] DB seed 재실행 절차 정리
- [ ] 실패 시나리오 대비
  - 로그인 실패
  - 컨디션 미입력 상태
  - 퀘스트 이미 생성된 상태
  - 스토리 세션이 이미 진행 중인 상태
  - 퀘스트 이미 완료된 상태

## 발표용 API 순서

```text
POST /api/auth/login
GET  /api/users/me
PUT  /api/users/me/exercise-profile
GET  /api/routines/default
GET  /api/routines/{routineId}
PUT  /api/conditions/today
POST /api/quests/today/generate
GET  /api/quests/today
POST /api/stories/start
POST /api/stories/play
GET  /api/quests/today
POST /api/quests/{questId}/complete
GET  /api/quests/today
GET  /api/users/me/summary
```

## 발표 리허설 통과 기준

- [ ] 새 DB 또는 seed 초기화 후 5분 안에 서버를 띄울 수 있다.
- [ ] 발표용 계정으로 로그인된다.
- [ ] 운동 경험 설정 후 루틴 화면에 더미 루틴이 보인다.
- [ ] 컨디션 입력 결과에 condition score와 exercise multiplier가 표시된다.
- [ ] 당일 퀘스트가 생성되고 중복 생성되지 않는다.
- [ ] 스토리 진행 중 분기 선택이 저장된다.
- [ ] 분기 선택 이후 퀘스트 카드에 스토리 맥락이 붙는다.
- [ ] 퀘스트 완료 시 보상과 완료 상태가 즉시 반영된다.
- [ ] 개발서버 `http://100.89.171.113:8080/actuator/health`가 `UP`이다.
- [ ] 발표 중 OpenAI API가 실패해도 더미 응답으로 시연이 계속된다.

## 구현 시 주의점

- 실제 비밀번호나 API 키는 커밋하지 않는다.
- `application.properties`에는 환경변수 참조만 둔다.
- 발표용 더미 데이터는 코드에서 명확히 `demo` 목적임을 표시한다.
- 복잡한 검증보다 상태 전이가 깨지지 않는 것을 우선한다.
- 퀘스트 완료와 보상 지급은 한 트랜잭션으로 묶는다.
- 스토리와 퀘스트 연결은 `user_quests` 확장 컬럼을 우선 사용한다.
