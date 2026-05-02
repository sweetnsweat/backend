# Backend Development Environment

## Local

- Java: Temurin 21.0.6
- Gradle: wrapper 9.4.1
- Spring Boot: 4.0.5
- Docker client/server: 28.5.1 / 27.4.0
- PostgreSQL container: `postgres-db`
- PostgreSQL image: `postgres:16`
- PostgreSQL version: 16.13
- PostgreSQL port: `localhost:5432`
- PostgreSQL database/user: `postgres` / `postgres`

## Development Server

- Tailscale host: `dy-minipc`
- Tailscale IP: `100.89.171.113`
- OS: Ubuntu 24.04
- Docker client: 29.4.0
- PostgreSQL container: `postgres-db`
- PostgreSQL image: `postgres:16`
- PostgreSQL version: 16.13
- PostgreSQL port: `0.0.0.0:5432->5432/tcp`
- PostgreSQL health check: `pg_isready -U postgres -d postgres`

## Run Backend

```bash
DB_PASSWORD=replace-with-shared-dev-password ./gradlew bootRun
```

The application reads PostgreSQL settings from environment variables, with local defaults in `src/main/resources/application.properties`.

## Run PostgreSQL With Docker Compose

```bash
cp .env.example .env
./gradlew bootJar
docker compose up -d --build
```

Use the shared development password in `.env` before starting the container.

This starts:

- `capstone-backend` from `capstone-backend:dev`
- `postgres-db` from `postgres:16`

## API Documentation

Swagger UI exposes both the generated Springdoc document and the shared static contract YAML.

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- Springdoc generated YAML: `http://localhost:8080/v3/api-docs.yaml`
- Shared contract YAML: `http://localhost:8080/openapi.yaml`
- Development server Swagger UI: `http://100.89.171.113:8080/swagger-ui/index.html`
- Development server Springdoc generated YAML: `http://100.89.171.113:8080/v3/api-docs.yaml`
- Development server shared contract YAML: `http://100.89.171.113:8080/openapi.yaml`

Swagger UI defaults to `midterm-contract-yaml` so the frontend can consume the planned midterm API contract before every controller is implemented.

## Development Logs

The backend writes application logs to `logs/backend.log` by default and exposes
them through a development-only tail endpoint and Spring Boot Actuator.

- Browser-friendly development log tail: `http://100.89.171.113:8080/dev/logs`
- Last N lines: `http://100.89.171.113:8080/dev/logs?lines=300`

- Local logs endpoint: `http://localhost:8080/actuator/logfile`
- Development server logs endpoint: `http://100.89.171.113:8080/actuator/logfile`

The `/dev/logs` endpoint is intended only for the Tailscale development server.
The Actuator logfile endpoint requires a JWT access token. For development, login with
the demo account and pass the returned access token as a Bearer token.

API requests under `/api/**` are written to the same log file when each request
finishes. Sensitive query parameters such as tokens and passwords are masked.

```text
API_REQUEST method=GET path=/api/users/me status=200 durationMs=34 client=127.0.0.1 user=demoUser(1)
```

For 4xx/5xx responses, the log also includes the request body and response body
when they are readable text/JSON. Sensitive fields such as passwords and tokens
are masked. For validation errors, check `responseBody.detail` and
`responseBody.errors` to see which input rules must be satisfied.

```text
API_REQUEST method=PUT path=/api/users/me/onboarding-profile status=400 durationMs=49 client=100.76.190.48 user=admin123(8) requestBody={...} responseBody={"code":"VALIDATION_ERROR","errors":[...]} debugHint=responseBody.detail_or_errors_are_validation_requirements
```

```bash
curl -s -X POST http://100.89.171.113:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"loginId":"demoUser","password":"password123"}'
```

```bash
curl http://100.89.171.113:8080/actuator/logfile \
  -H "Authorization: Bearer <accessToken>"
```
