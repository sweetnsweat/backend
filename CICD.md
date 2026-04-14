# CI/CD Plan

## Goal

Use Jenkins to keep the development server synchronized with the `main` branch of `sweetnsweat/backend`.

Target flow:

```text
GitHub sweetnsweat/backend main
-> Jenkins job sweetnsweat-backend-main
-> Gradle test + bootJar
-> Docker image capstone-backend:dev
-> development server container capstone-backend
-> health and Swagger endpoint verification
```

## Current Development Server

- Host: `dy-minipc`
- Tailscale IP: `100.89.171.113`
- Backend container: `capstone-backend`
- Backend image: `capstone-backend:dev`
- Backend port: `8080`
- PostgreSQL container: `postgres-db`
- PostgreSQL Docker network: `postgres-stack_default`
- Jenkins container: `sweetnsweat-jenkins`
- GitHub webhook relay container: `sweetnsweat-github-webhook-relay`
- Jenkins URL: `http://100.89.171.113:8081`
- GitHub webhook relay local URL: `http://100.89.171.113:3000/webhooks/github`

## Backend Jenkins Job

The Jenkins job is created by `ci/jenkins/init.groovy.d/backend-pipeline.groovy`.

- Job name: `sweetnsweat-backend-main`
- Repository: `git@github.com:sweetnsweat/backend.git`
- Branch: `main`
- Jenkinsfile path: `Jenkinsfile`
- Git credential ID: `github-token`
- DB password credential ID: `backend-db-password`

The pipeline uses both:

- `githubPush()` for GitHub webhook events
- `pollSCM('H/2 * * * *')` as a fallback if the Funnel endpoint is down

## Backend Deploy Steps

`Jenkinsfile` performs:

1. `./gradlew clean test bootJar`
2. `docker build -t capstone-backend:dev .`
3. Remove the old `capstone-backend` container if it exists
4. Start the new container on `postgres-stack_default`
5. Verify:
   - `/actuator/health`
   - `/api/health`
   - `/swagger-ui/index.html`
   - `/v3/api-docs.yaml`
   - `/openapi.yaml`

Jenkins runs inside a container, so verification calls the backend through `host.docker.internal:8080` rather than `localhost:8080`.

## Jenkins Server Bootstrap

Jenkins is defined under `ci/jenkins`.

Required server-only file:

- `/home/dy/jenkins-backend-cicd/.env`

The GitHub token and DB password are not committed. They live only in the Jenkins server `.env`.

The first attempt used a read-only deploy key, but deploy keys are disabled for `sweetnsweat/backend`, so Jenkins checkout uses a GitHub token credential instead.

Example `.env` on the development server:

```bash
JENKINS_ADMIN_ID=admin
JENKINS_ADMIN_PASSWORD=change-this-on-server
GITHUB_TOKEN=github-token-with-repo-read-access
BACKEND_DB_PASSWORD=change-this-on-server
```

Start Jenkins:

```bash
cd ~/jenkins-backend-cicd
docker compose up -d --build
```

The Jenkins container mounts the host Docker socket and Docker CLI. This is acceptable only for the development server because it gives Jenkins deployment-level access to Docker on that host.

## GitHub Webhook With Tailscale Funnel

GitHub cannot call the private Tailscale IP directly. The development server therefore runs a small nginx relay on port `3000` that only forwards `/webhooks/github` to Jenkins' `/github-webhook/`.

Local relay target:

```text
http://100.89.171.113:3000/webhooks/github
```

Expose it through Tailscale Funnel:

```bash
tailscale funnel --bg 3000
tailscale funnel status
```

Register the public Funnel URL as a GitHub webhook:

```text
https://<your-node>.<tailnet>.ts.net/webhooks/github
```

Webhook settings:

- Repository: `sweetnsweat/backend`
- Payload URL: Funnel URL ending in `/webhooks/github`
- Content type: `application/json`
- Event: `push`
- Active: yes

Do not expose Jenkins port `8081` directly through Funnel unless there is a specific reason. The relay keeps the public surface limited to the webhook path.

## Frontend and AI Server Extension Plan

Use the same pattern for `frontend` and `ai` repositories.

Recommended job names:

- `sweetnsweat-frontend-main`
- `sweetnsweat-ai-main`

Recommended repository layout per service:

- `Jenkinsfile`
- `Dockerfile`
- service-specific `.env.example`
- service-specific `CICD.md` notes if needed

Recommended container names:

- `capstone-frontend`
- `capstone-ai`

Recommended images:

- `capstone-frontend:dev`
- `capstone-ai:dev`

Recommended ports:

- Frontend: `3000` or `5173`, depending on the selected frontend runtime
- AI server: `8000`

Recommended Jenkins credentials:

- `frontend-env-file` or explicit secret text credentials per frontend secret
- `ai-env-file` or explicit secret text credentials per AI secret
- separate GitHub deploy keys per repository if the organization allows them, otherwise separate token credentials

Avoid reusing one deploy key or token across multiple repositories. Keep access scoped per repository so it can be revoked independently.
