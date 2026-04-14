pipeline {
    agent any

    triggers {
        pollSCM('H/2 * * * *')
    }

    environment {
        APP_CONTAINER = 'capstone-backend'
        APP_IMAGE = 'capstone-backend:dev'
        APP_PORT = '8080'
        DOCKER_NETWORK = 'postgres-stack_default'
        DB_URL = 'jdbc:postgresql://postgres-db:5432/postgres'
        DB_USERNAME = 'postgres'
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
                      code="$(curl -s -o /tmp/jenkins-verify.out -w '%{http_code}' "http://localhost:${APP_PORT}${endpoint}")"
                      test "$code" = "200"
                    done
                '''
            }
        }
    }
}
