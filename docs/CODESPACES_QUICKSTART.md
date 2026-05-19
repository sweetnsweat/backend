# Codespaces Quickstart

외부 컴퓨터에서 GitHub Codespaces로 백엔드 개발을 바로 이어가기 위한 최소 절차다.

상세 맥락은 `docs/CODESPACES_BACKEND_HANDOFF_20260519.md`를 먼저 읽는다.

## 1. Codespace 열기

GitHub에서 `sweetnsweat/backend` 레포의 `main` 브랜치로 Codespace를 생성한다.

기존 Codespace를 재사용하는데 `.devcontainer/devcontainer.json` 변경이 반영되지 않았다면 Codespaces UI에서 rebuild container를 실행한다.

## 2. 기본 상태 확인

```bash
git status --short --branch
git pull --ff-only origin main
chmod +x ./gradlew
```

## 3. Tailscale 확인

자동 로그인을 쓰려면 GitHub Codespaces secret에 `TS_AUTH_KEY`가 있어야 한다.

```bash
tailscale status
```

로그인이 안 되어 있으면 수동 로그인한다.

```bash
sudo tailscale up --accept-routes
```

개발 서버 접근 확인:

```bash
curl http://100.89.171.113:8080/api/health
ssh 100.89.171.113 "docker ps --format '{{.Names}} {{.Status}}'"
```

Tailscale이 꼭 필요한 작업:

- 개발 서버 컨테이너 확인
- Jenkins 배포 결과를 서버에서 직접 확인
- 개발 서버 PostgreSQL/Redis/AI 서버 접근

로컬 Codespaces DB만 쓰는 백엔드 개발은 Tailscale 없이도 가능하다.

## 4. 로컬 DB/Redis 실행

```bash
docker compose up -d postgres redis
docker ps --format '{{.Names}} {{.Status}}'
```

Postgres 준비 확인:

```bash
docker exec postgres-db pg_isready -U postgres -d postgres
```

## 5. 개발 서버 DB dump 복원

전체 개발 서버 dump 파일:

```text
db/codespaces/dev_server_postgres_full_20260519.sql.gz
```

복원:

```bash
gzip -cd db/codespaces/dev_server_postgres_full_20260519.sql.gz | docker exec -i postgres-db psql -U postgres -d postgres -v ON_ERROR_STOP=1
```

확인:

```bash
docker exec postgres-db psql -U postgres -d postgres -Atc "select count(*) from users"
docker exec postgres-db psql -U postgres -d postgres -Atc "select count(*) from scenarios"
docker exec postgres-db psql -U postgres -d postgres -Atc "select count(*) from exercises"
```

주의: 이 dump는 전체 개발 DB라 사용자, 토큰, 푸시토큰, 스토리 로그가 포함되어 있다. private repo/Codespaces 재현용으로만 사용한다.

## 6. 백엔드 실행

Gradle로 실행:

```bash
./gradlew bootRun
```

컨테이너로 실행:

```bash
docker compose up -d --build
```

## 7. 백엔드 확인

```bash
curl http://localhost:8080/api/health
curl http://localhost:8080/v3/api-docs.yaml | head
```

브라우저:

```text
http://localhost:8080/swagger-ui/index.html
```

Codespaces 포트 탭에서 `8080` 포트가 forward 되어 있는지도 확인한다.

## 8. 배포 확인 흐름

`main`에 push하면 Jenkins가 개발 서버 컨테이너를 재배포한다.

Codespaces에서 Tailscale이 붙어 있으면 다음으로 확인한다.

```bash
curl http://100.89.171.113:8080/api/health
curl http://100.89.171.113:8080/v3/api-docs.yaml | head
ssh 100.89.171.113 "docker ps --format '{{.Names}} {{.Image}} {{.Status}}'"
ssh 100.89.171.113 "docker logs --tail 80 capstone-backend"
```

## 9. 자주 막히는 지점

- `TS_AUTH_KEY`가 없으면 Tailscale 자동 로그인은 안 된다. 수동 `sudo tailscale up --accept-routes`를 쓴다.
- DB dump 복원은 기존 Codespaces DB를 덮어쓴다.
- AI 서버 테스트는 이 quickstart 범위에서 제외한다.
- FCM은 Codespaces에서 `FIREBASE_ENABLED=false` 기준이다. 실제 FCM 발송은 개발 서버/Jenkins 설정을 기준으로 본다.
- 기존 로컬 Mac의 Colima, Android emulator, 로컬 Docker 상태는 Codespaces로 넘어오지 않는다.
