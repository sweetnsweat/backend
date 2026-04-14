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
