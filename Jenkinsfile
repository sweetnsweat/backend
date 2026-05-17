pipeline {
    agent any

    options {
        disableConcurrentBuilds()
    }

    triggers {
        githubPush()
        pollSCM('H/2 * * * *')
    }

    environment {
        APP_CONTAINER = 'capstone-backend'
        APP_IMAGE = 'capstone-backend:dev'
        APP_PORT = '8080'
        REDIS_CONTAINER = 'redis-cache'
        REDIS_IMAGE = 'redis:7'
        REDIS_HOST = 'redis-cache'
        REDIS_PORT = '6379'
        DOCKER_NETWORK = 'postgres-stack_default'
        DB_URL = 'jdbc:postgresql://postgres-db:5432/postgres'
        DB_USERNAME = 'postgres'
        AI_BASE_URL = 'http://capstone-ai:8000'
        MEDIA_BASE_URL = 'http://100.89.171.113:8000'
        MAIL_HOST = 'smtp.naver.com'
        MAIL_PORT = '465'
        MAIL_USERNAME = '4pril17@naver.com'
        MAIL_FROM = '4pril17@naver.com'
        MAIL_FROM_NAME = 'sweet & sweat'
        MAIL_SMTP_AUTH = 'true'
        MAIL_SMTP_SSL_ENABLE = 'true'
        MAIL_SMTP_STARTTLS_ENABLE = 'false'
        FIREBASE_ENABLED = 'true'
        FIREBASE_PROJECT_ID = 'sweetnsweat-9c052'
        FIREBASE_SERVICE_ACCOUNT_HOST_PATH = '/home/dy/secrets/firebase-service-account.json'
        FIREBASE_SERVICE_ACCOUNT_CONTAINER_PATH = '/run/secrets/firebase-service-account.json'
        VERIFY_BASE_URL = 'http://host.docker.internal:8080'
    }

    stages {
        stage('Build and Test') {
            steps {
                sh 'chmod +x ./gradlew'
                sh '''
                    unset DB_URL DB_USERNAME DB_PASSWORD
                    ./gradlew clean test bootJar
                '''
            }
        }

        stage('Build Docker Image') {
            steps {
                sh 'docker build -t ${APP_IMAGE} .'
            }
        }

        stage('Deploy to Development Server') {
            steps {
                withCredentials([string(credentialsId: 'backend-db-password', variable: 'DB_PASSWORD')]) {
                    sh '''
                        set -eu
                        docker pull "${REDIS_IMAGE}"
                        docker rm -f "${REDIS_CONTAINER}" >/dev/null 2>&1 || true
                        docker rm -f "${APP_CONTAINER}" >/dev/null 2>&1 || true

                        docker run -d \
                          --name "${REDIS_CONTAINER}" \
                          --network "${DOCKER_NETWORK}" \
                          --restart unless-stopped \
                          "${REDIS_IMAGE}" \
                          redis-server --appendonly yes

                        docker run -d \
                          --name "${APP_CONTAINER}" \
                          --network "${DOCKER_NETWORK}" \
                          --restart unless-stopped \
                          -p "${APP_PORT}:8080" \
                          -v "${FIREBASE_SERVICE_ACCOUNT_HOST_PATH}:${FIREBASE_SERVICE_ACCOUNT_CONTAINER_PATH}:ro" \
                          -e SERVER_PORT=8080 \
                          -e DB_URL="${DB_URL}" \
                          -e DB_USERNAME="${DB_USERNAME}" \
                          -e DB_PASSWORD="${DB_PASSWORD}" \
                          -e REDIS_HOST="${REDIS_HOST}" \
                          -e REDIS_PORT="${REDIS_PORT}" \
                          -e AI_BASE_URL="${AI_BASE_URL}" \
                          -e MEDIA_BASE_URL="${MEDIA_BASE_URL}" \
                          -e MAIL_HOST="${MAIL_HOST}" \
                          -e MAIL_PORT="${MAIL_PORT}" \
                          -e MAIL_USERNAME="${MAIL_USERNAME}" \
                          -e MAIL_PASSWORD="${MAIL_PASSWORD:-}" \
                          -e MAIL_FROM="${MAIL_FROM}" \
                          -e MAIL_FROM_NAME="${MAIL_FROM_NAME}" \
                          -e MAIL_SMTP_AUTH="${MAIL_SMTP_AUTH}" \
                          -e MAIL_SMTP_SSL_ENABLE="${MAIL_SMTP_SSL_ENABLE}" \
                          -e MAIL_SMTP_STARTTLS_ENABLE="${MAIL_SMTP_STARTTLS_ENABLE}" \
                          -e FIREBASE_ENABLED="${FIREBASE_ENABLED}" \
                          -e FIREBASE_PROJECT_ID="${FIREBASE_PROJECT_ID}" \
                          -e FIREBASE_SERVICE_ACCOUNT_PATH="${FIREBASE_SERVICE_ACCOUNT_CONTAINER_PATH}" \
                          "${APP_IMAGE}"
                    '''
                }
            }
        }

        stage('Verify') {
            steps {
                sh '''
                    set -eu
                    for endpoint in \
                      /actuator/health \
                      /api/health \
                      /swagger-ui/index.html \
                      /v3/api-docs.yaml \
                      /openapi.yaml
                    do
                      ok=0
                      for attempt in $(seq 1 30); do
                        code="$(curl -s -o /tmp/jenkins-verify.out -w '%{http_code}' "${VERIFY_BASE_URL}${endpoint}" || true)"
                        if [ "$code" = "200" ]; then
                          ok=1
                          break
                        fi
                        sleep 2
                      done
                      if [ "$ok" != "1" ]; then
                        echo "Verification failed for ${endpoint}; last HTTP status was ${code}"
                        cat /tmp/jenkins-verify.out || true
                        exit 1
                      fi
                    done
                '''
            }
        }

        stage('Verify Auth Flow') {
            steps {
                sh '''
                    set -eu
                    tmp_prefix="/tmp/jenkins-auth-${BUILD_NUMBER}"

                    probe_login_id="jenkins_probe_${BUILD_NUMBER}_$(date +%s)"
                    probe_nickname="JenkinsProbe${BUILD_NUMBER}"
                    password='password123'

                    signup_payload=$(printf '{"loginId":"%s","password":"%s","nickname":"%s"}' "$probe_login_id" "$password" "$probe_nickname")
                    signup_code="$(curl -s -o "${tmp_prefix}-signup.out" -w '%{http_code}' \
                      -X POST "${VERIFY_BASE_URL}/api/auth/signup" \
                      -H 'Content-Type: application/json' \
                      -d "$signup_payload" || true)"

                    if [ "$signup_code" != "201" ]; then
                      echo "Auth verify failed at signup; status=${signup_code}"
                      cat "${tmp_prefix}-signup.out" || true
                      exit 1
                    fi

                    login_payload=$(printf '{"loginId":"%s","password":"%s"}' "$probe_login_id" "$password")
                    login_code="$(curl -s -o "${tmp_prefix}-login.out" -w '%{http_code}' \
                      -X POST "${VERIFY_BASE_URL}/api/auth/login" \
                      -H 'Content-Type: application/json' \
                      -d "$login_payload" || true)"

                    if [ "$login_code" != "200" ]; then
                      echo "Auth verify failed at login; status=${login_code}"
                      cat "${tmp_prefix}-login.out" || true
                      exit 1
                    fi

                    access_token="$(sed -n 's/.*"accessToken":"\\([^"]*\\)".*/\\1/p' "${tmp_prefix}-login.out" | head -n 1)"
                    if [ -z "$access_token" ]; then
                      echo "Auth verify failed: accessToken missing in login response"
                      cat "${tmp_prefix}-login.out" || true
                      exit 1
                    fi

                    ai_health_code="$(curl -s -o "${tmp_prefix}-ai-health.out" -w '%{http_code}' \
                      "${VERIFY_BASE_URL}/api/ai/health" \
                      -H "Authorization: Bearer ${access_token}" || true)"

                    if [ "$ai_health_code" != "200" ]; then
                      echo "AI proxy verify failed; status=${ai_health_code}"
                      cat "${tmp_prefix}-ai-health.out" || true
                      exit 1
                    fi

                    logout_code="$(curl -s -o "${tmp_prefix}-logout.out" -w '%{http_code}' \
                      -X POST "${VERIFY_BASE_URL}/api/auth/logout" \
                      -H "Authorization: Bearer ${access_token}" || true)"

                    if [ "$logout_code" != "200" ]; then
                      echo "Auth verify failed at logout; status=${logout_code}"
                      cat "${tmp_prefix}-logout.out" || true
                      exit 1
                    fi
                '''
            }
        }
    }
}
