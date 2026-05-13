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

        stage('Build Docker Image') {
            steps {
                script {
                    docker.build("${params.DOCKER_IMAGE}:${env.DOCKER_TAG}")
                }
            }
        }

        stage('Push Docker Image') {
            steps {
                script {
                    docker.withRegistry('https://index.docker.io/v1/', params.DOCKER_HUB_CREDENTIALS_ID) {
                        def image = docker.image("${params.DOCKER_IMAGE}:${env.DOCKER_TAG}")
                        image.push()
                        image.push('latest')
                    }
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
