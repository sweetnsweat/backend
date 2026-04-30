# 컨디션 점수 모델 근거

## 목적

`PUT /api/conditions/today`에서 입력받은 주관 컨디션 데이터를 0~100점의 `conditionScore`로 변환하고, 이 점수를 기반으로 당일 운동 루틴의 강도 보정값인 `exerciseMultiplier`를 산출한다.

이 모델은 진단이나 의학적 판단을 위한 모델이 아니라, 운동 루틴 추천에서 당일 컨디션을 반영하기 위한 readiness score 정책이다.

## 참고 모델

현재 모델은 `Hooper Index` 계열의 주관 웰니스 모니터링 방식을 참고한다.

Hooper Index는 운동 선수의 훈련 전 상태를 모니터링하기 위해 다음과 같은 주관 지표를 사용한다.

- 수면 상태
- 스트레스
- 피로
- 근육통 또는 신체 상태

연구에서는 이러한 항목을 훈련 전 설문으로 수집해 피로, 회복 상태, 훈련 부하 조절의 참고 지표로 사용한다. 대표적으로 프로 농구 선수 대상 연구에서는 수면, 스트레스, 피로, 근육통을 1~7 척도로 측정하고, 네 항목의 합을 Hooper index로 사용했다.

우리 서비스의 현재 UI는 근육통 항목을 직접 받지 않고, 대신 사용자가 직접 고른 오늘 컨디션과 에너지 레벨을 받는다. 따라서 원형 Hooper Index를 그대로 쓰지는 않고, 앱 입력 구조에 맞춘 modified Hooper-style readiness score로 적용한다.

## 입력값

현재 컨디션 입력 API는 다음 네 가지 값을 받는다.

```json
{
  "conditionLevel": 4,
  "sleepScore": 3,
  "stressScore": 2,
  "energyLevel": 4
}
```

각 항목의 의미는 다음과 같다.

- `conditionLevel`: 사용자가 직접 선택한 오늘 컨디션, 1~5
- `sleepScore`: 전날 수면 상태, 1~4
- `stressScore`: 스트레스 정도, 1~5
- `energyLevel`: 현재 에너지 레벨, 1~5

`stressScore`는 값이 클수록 스트레스가 높은 부정 지표다. 나머지 값은 클수록 좋은 긍정 지표다.

## 점수 산식

각 항목을 0~100점으로 정규화한 뒤 동일 가중 평균을 낸다.

```text
condition = (conditionLevel - 1) / 4 * 100
sleep     = (sleepScore - 1) / 3 * 100
stress    = (5 - stressScore) / 4 * 100
energy    = (energyLevel - 1) / 4 * 100

conditionScore = average(condition, sleep, stress, energy)
```

예시 입력:

```json
{
  "conditionLevel": 4,
  "sleepScore": 3,
  "stressScore": 2,
  "energyLevel": 4
}
```

계산:

```text
condition = 75.00
sleep     = 66.67
stress    = 75.00
energy    = 75.00

conditionScore = 72.92
```

## 운동 강도 배율

`conditionScore`에 따라 운동 루틴의 강도 보정값을 산출한다.

```text
0점 이상 40점 미만   -> 0.70
40점 이상 60점 미만  -> 0.85
60점 이상 80점 미만  -> 1.00
80점 이상            -> 1.10
```

해석은 다음과 같다.

- `0.70`: 컨디션이 매우 낮으므로 회복 중심으로 운동량을 줄인다.
- `0.85`: 컨디션이 낮으므로 기본 운동량보다 낮게 수행한다.
- `1.00`: 일반적인 컨디션으로 기본 루틴을 유지한다.
- `1.10`: 컨디션이 좋은 날이므로 보수적으로 운동량을 증가시킨다.

## 배율 근거

증량 상한은 ACSM의 저항운동 progression model을 참고해 10%로 제한했다.

ACSM progression model은 사용자가 현재 운동량을 안정적으로 수행할 수 있을 때 부하를 2~10% 범위에서 증가시키는 방식을 제시한다. 따라서 컨디션이 좋은 날에도 `1.15` 같은 공격적인 증량 대신 `1.10`을 상한으로 둔다.

감량 배율인 `0.70`, `0.85`는 Hooper-style readiness score를 이용한 앱 내부 정책값이다. 낮은 readiness 상태에서는 부하를 줄여 회복 우선 루틴으로 유도하기 위한 값이며, 향후 실제 운동 수행 데이터와 웨어러블 데이터가 쌓이면 개인화 보정 대상이다.

## 현재 구현 위치

- 계산 로직: `src/main/java/com/capstone/backend/condition/service/ConditionService.java`
- 요청 DTO: `src/main/java/com/capstone/backend/condition/dto/ConditionTodayRequest.java`
- 응답 DTO: `src/main/java/com/capstone/backend/condition/dto/ConditionLogResponse.java`
- API 컨트롤러: `src/main/java/com/capstone/backend/condition/controller/ConditionController.java`
- 테스트: `src/test/java/com/capstone/backend/condition/controller/ConditionControllerTest.java`

## 한계와 향후 개선

현재 모델은 검증된 생체 신호 기반 모델이 아니라, 검증된 주관 웰니스 모니터링 프레임워크를 앱 입력 구조에 맞춘 정책 모델이다.

향후 개선 방향은 다음과 같다.

- 근육통 또는 운동 후 통증 항목을 추가해 Hooper Index 원형에 더 가깝게 맞춘다.
- 웨어러블 데이터의 수면 시간, 안정시 심박수, HRV, 활동량을 반영한다.
- 사용자의 최근 운동 수행률과 운동 후 피드백을 이용해 개인별 multiplier를 보정한다.
- 절대 점수뿐 아니라 최근 7일 평균 대비 변화량을 함께 사용한다.

## 참고 자료

- Perceived Training Load, Muscle Soreness, Stress, Fatigue, and Sleep Quality in Professional Basketball: A Full Season Study
  https://pmc.ncbi.nlm.nih.gov/articles/PMC6714361/

- ACSM Progression Models in Resistance Training for Healthy Adults
  https://tourniquets.org/wp-content/uploads/PDFs/ACSM-Progression-models-in-resistance-training-for-healthy-adults-2009.pdf

- ACSM Resistance Training Guidelines Update
  https://acsm.org/resistance-training-guidelines-update-2026/
