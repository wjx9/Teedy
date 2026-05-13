pipeline {
    agent any

    parameters {
        string(name: 'DOCKER_IMAGE', defaultValue: 'wjx9/teedy', description: 'Docker Hub repository')
        string(name: 'DOCKER_HUB_CREDENTIALS_ID', defaultValue: '123', description: 'Jenkins Docker Hub credential ID')
    }

    environment {
        JAVA_HOME = "/Library/Java/JavaVirtualMachines/amazon-corretto-11.jdk/Contents/Home"
        PATH = "/Library/Java/JavaVirtualMachines/amazon-corretto-11.jdk/Contents/Home/bin:/opt/homebrew/bin:/usr/local/bin:${env.PATH}"
        MAVEN_ARGS = "-B -ntp"
        DOCS_DEFAULT_LANGUAGE = "en"
        DOCS_BASE_URL = "http://localhost/docs"
        DOCKER_TAG = "${env.BUILD_NUMBER}"
    }

    options {
        disableConcurrentBuilds()
        timestamps()
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build Teedy') {
            steps {
                sh 'mvn ${MAVEN_ARGS} -DskipTests clean package'
            }
        }

        stage('Prepare Docker') {
            steps {
                sh '''
                    if docker info >/dev/null 2>&1; then
                        echo "Docker daemon is ready."
                        exit 0
                    fi

                    echo "Docker daemon is not ready. Trying to start Docker Desktop..."
                    if command -v open >/dev/null 2>&1; then
                        open -a Docker || true
                    fi

                    for attempt in $(seq 1 60); do
                        if docker info >/dev/null 2>&1; then
                            echo "Docker daemon is ready."
                            exit 0
                        fi
                        echo "Waiting for Docker daemon... ${attempt}/60"
                        sleep 2
                    done

                    echo "Docker daemon is still unavailable. Start Docker Desktop, then rebuild."
                    docker info
                '''
            }
        }

        stage('Build Docker Image') {
            steps {
                sh 'docker build -t "${DOCKER_IMAGE}:${DOCKER_TAG}" .'
            }
        }

        stage('Push Docker Image') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: params.DOCKER_HUB_CREDENTIALS_ID,
                    usernameVariable: 'DOCKER_HUB_USERNAME',
                    passwordVariable: 'DOCKER_HUB_PASSWORD'
                )]) {
                    sh '''
                        echo "${DOCKER_HUB_PASSWORD}" | docker login -u "${DOCKER_HUB_USERNAME}" --password-stdin
                        docker push "${DOCKER_IMAGE}:${DOCKER_TAG}"
                        docker tag "${DOCKER_IMAGE}:${DOCKER_TAG}" "${DOCKER_IMAGE}:latest"
                        docker push "${DOCKER_IMAGE}:latest"
                    '''
                }
            }
        }

        stage('Run Containers') {
            steps {
                sh '''
                    for port in 8082 8083 8084; do
                        docker rm -f "teedy-container-${port}" || true
                        docker run -d \
                            --name "teedy-container-${port}" \
                            -p "${port}:8080" \
                            "${DOCKER_IMAGE}:${DOCKER_TAG}"
                    done
                    docker ps --filter "name=teedy-container"
                '''
            }
        }
    }

    post {
        always {
            archiveArtifacts artifacts: '**/target/**/*.war', fingerprint: true, allowEmptyArchive: true
        }
    }
}
