# Performance Test - 2026-05-26

## Purpose

최종 발표의 성능 분석 자료로 사용할 API 응답 시간과 동시 요청 안정성을 측정했다.

AI 스토리 생성 API는 외부 LLM 호출 시간이 지배적이므로 이번 측정에서는 제외했다. 테스트 대상은 백엔드와 DB가 직접 처리하는 핵심 조회 API로 한정했다.

## Environment

- Date: 2026-05-26
- Target: `http://100.89.171.113:8080`
- Server: 개발 서버 Docker 환경
- Hostname: `dy-miniPC`
- CPU: Intel N150, 4 cores / 4 threads
- Memory: 15GiB
- Disk: 468GiB total, 367GiB available at measurement time
- Docker: 29.4.0
- Container resource limit: 별도 CPU/Memory 제한 없음
- Tool: k6
- Auth: `admin123` 계정으로 로그인 후 Bearer Token 사용
- Script: `performance/backend-core-api.k6.js`

## Target APIs

| Endpoint | Purpose |
| --- | --- |
| `GET /api/users/me` | 사용자 프로필 조회 |
| `GET /api/shop/items?type=character` | 상점 캐릭터 아이템 조회 |
| `GET /api/worlds/rankings?limit=5` | 세계관 랭킹 조회 |
| `GET /api/stories/chats?limit=20` | 스토리 채팅 목록 조회 |
| `GET /api/routines/today` | 오늘 루틴 조회 |
| `GET /api/battles/me/summary` | 배틀 요약 조회 |
| `GET /api/records/stats?period=WEEKLY` | 주간 기록 통계 조회 |

## How To Run

```bash
PERF_PASSWORD=password123 \
PERF_MODE=latency \
PERF_ITERATIONS=30 \
PERF_VUS=1 \
k6 run performance/backend-core-api.k6.js
```

```bash
PERF_PASSWORD=password123 \
PERF_MODE=load \
PERF_VUS=30 \
PERF_DURATION=60s \
k6 run performance/backend-core-api.k6.js
```

## Result 1. API Response Time

조건:

- VUs: 1
- Iterations: 30
- Total requests: 211
- Failure rate: 0%

| API | Avg | p90 | p95 | Max |
| --- | ---: | ---: | ---: | ---: |
| 사용자 프로필 | 17.90ms | 20.50ms | 20.93ms | 21.70ms |
| 상점 캐릭터 아이템 | 25.94ms | 33.39ms | 33.53ms | 39.14ms |
| 세계관 랭킹 | 20.71ms | 26.58ms | 29.89ms | 36.68ms |
| 스토리 채팅 목록 | 18.99ms | 23.74ms | 25.73ms | 27.73ms |
| 오늘 루틴 | 22.13ms | 31.05ms | 31.71ms | 38.43ms |
| 배틀 요약 | 17.87ms | 24.08ms | 26.04ms | 27.41ms |
| 주간 기록 통계 | 16.22ms | 21.83ms | 24.93ms | 31.78ms |

Overall:

- Average: 20.33ms
- p95: 31.98ms
- Max: 96.87ms
- Failure rate: 0%

## Result 2. Concurrent Request Stability

조건:

- VUs: 30
- Duration: 60s
- Total requests: 27,623
- Throughput: 457.75 req/s
- Failure rate: 0%
- Check success: 100%

| API | Avg | p90 | p95 | Max |
| --- | ---: | ---: | ---: | ---: |
| 사용자 프로필 | 46.93ms | 73.63ms | 86.61ms | 308.76ms |
| 상점 캐릭터 아이템 | 63.30ms | 93.63ms | 108.50ms | 268.14ms |
| 세계관 랭킹 | 59.11ms | 88.95ms | 105.79ms | 221.47ms |
| 스토리 채팅 목록 | 58.16ms | 86.58ms | 103.26ms | 309.78ms |
| 오늘 루틴 | 67.94ms | 101.15ms | 120.20ms | 377.18ms |
| 배틀 요약 | 59.30ms | 89.81ms | 104.73ms | 272.54ms |
| 주간 기록 통계 | 51.35ms | 78.32ms | 93.27ms | 204.91ms |

Overall:

- Average: 58.01ms
- p95: 104.19ms
- Max: 377.18ms
- Failure rate: 0%

## Result 3. Concurrent User Capacity

동시 사용자 수를 10명부터 600명까지 늘리며 동일한 조회 API 묶음을 반복 호출했다. 각 VU는 한 명의 활성 사용자처럼 7개 핵심 조회 API를 순차 호출하므로, 실제 사용자가 화면을 천천히 탐색하는 상황보다 강한 부하에 가깝다.

안정 기준:

- Failure rate < 1%
- Overall p95 < 800ms
- 컨테이너 재시작/장애 없음

| Concurrent VUs | Requests | Throughput | Failure Rate | Avg | p95 | Max |
| ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| 10 | 24,515 | 407 req/s | 0% | 17.14ms | 30.91ms | 167.05ms |
| 30 | 27,623 | 458 req/s | 0% | 58.01ms | 104.19ms | 377.18ms |
| 50 | 55,679 | 923 req/s | 0% | 46.56ms | 85.21ms | 190.59ms |
| 100 | 65,157 | 1,074 req/s | 0% | 85.16ms | 141.48ms | 336.29ms |
| 150 | 60,369 | 993 req/s | 0% | 142.27ms | 219.51ms | 551.21ms |
| 200 | 59,669 | 981 req/s | 0% | 194.47ms | 318.06ms | 2,798.75ms |
| 300 | 61,769 | 1,011 req/s | 0% | 285.97ms | 425.99ms | 1,500.85ms |
| 500 | 62,931 | 1,021 req/s | 0% | 474.04ms | 687.10ms | 4,579.14ms |
| 600 | 62,154 | 1,000 req/s | 0% | 581.20ms | 1,026.93ms | 6,582.79ms |

Conclusion:

- 오류율 기준으로는 600명까지도 0%를 유지했다.
- 응답 시간 기준까지 포함하면 500명은 overall p95 687.10ms로 기준을 통과했다.
- 600명에서는 overall p95가 1.02s로 상승해 응답 시간 기준을 초과했다.
- 따라서 이번 개발 서버 환경에서 핵심 조회 API는 500명 동시 부하까지 오류 없이 처리했으며, 발표에서는 보수적으로 300명까지 안정 구간, 500명은 한계 근처 수용 가능 구간으로 설명하는 것이 적절하다.
- 처리량은 100명 이후 약 1,000 req/s 부근에서 포화되므로, 이후 VU 증가는 처리량 증가보다 대기 시간 증가로 나타났다.

## Server Resource Snapshot During Load

30 VU 부하 테스트 중 `docker stats`를 5초 간격으로 수집했다.

| Container | Peak CPU | Peak Memory |
| --- | ---: | ---: |
| `capstone-backend` | 327.07% | 793.5MiB |
| `postgres-db` | 56.15% | 205.4MiB |
| `redis-cache` | 2.91% | 4.2MiB |

## Presentation Summary

핵심 조회 API는 단일 요청 기준 평균 20.33ms, p95 31.98ms로 응답했다. 30명 동시 사용자 부하 조건에서도 총 27,623건 요청을 처리했고 실패율은 0%였다. 동시 요청 상태의 전체 평균 응답 시간은 58.01ms, p95는 104.19ms로 유지되어 일반 백엔드 API는 안정적으로 처리되는 것을 확인했다.

동시 사용자 수를 600명까지 단계적으로 높인 결과, 500명까지는 실패율 0%와 p95 687.10ms를 유지했다. 600명에서는 실패율은 0%였지만 p95가 1.02초로 상승해 응답 시간 기준을 초과했다. 따라서 개발 서버 기준 핵심 조회 API의 안정 수용 범위는 보수적으로 300명, 한계 근처 수용 가능 범위는 500명으로 분석했다.

AI 스토리 진행 API는 외부 LLM 응답 시간이 포함되므로 별도 성능 항목으로 분리해 설명하는 것이 적절하다.
