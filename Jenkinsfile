pipeline {
    agent any

    triggers {
        githubPush()
        pollSCM('H/2 * * * *')
    }

    environment {
        APP_CONTAINER = 'capstone-backend'
        APP_IMAGE = 'capstone-backend:dev'
        APP_PORT = '8080'
        DOCKER_NETWORK = 'postgres-stack_default'
        DB_URL = 'jdbc:postgresql://postgres-db:5432/postgres'
        DB_USERNAME = 'postgres'
        VERIFY_BASE_URL = 'http://host.docker.internal:8080'
    }

    stages {
        stage('Build and Test') {
            steps {
                sh 'chmod +x ./gradlew'
                sh './gradlew clean test bootJar'
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
                        docker rm -f "${APP_CONTAINER}" >/dev/null 2>&1 || true
                        docker run -d \
                          --name "${APP_CONTAINER}" \
                          --network "${DOCKER_NETWORK}" \
                          -p "${APP_PORT}:8080" \
                          -e SERVER_PORT=8080 \
                          -e DB_URL="${DB_URL}" \
                          -e DB_USERNAME="${DB_USERNAME}" \
                          -e DB_PASSWORD="${DB_PASSWORD}" \
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
    }
}
