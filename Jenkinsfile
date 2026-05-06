pipeline {
    agent any

    environment {
        PATH = "/opt/homebrew/bin:/usr/local/bin:${env.PATH}"
    }

    options {
        timestamps()
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Clean') {
            steps {
                sh 'mvn clean'
            }
        }

        stage('Compile') {
            steps {
                sh 'mvn compile'
            }
        }

        stage('Test') {
            steps {
                sh 'mvn test -Dmaven.test.failure.ignore=true'
            }
        }

        stage('PMD') {
            steps {
                sh 'mvn install -DskipTests pmd:pmd'
            }
        }

        stage('JaCoCo') {
            steps {
                sh 'mvn jacoco:report'
            }
        }

        stage('Site') {
            steps {
                sh 'mvn site site:stage -DskipTests'
            }
        }

        stage('Package') {
            steps {
                sh 'mvn package -DskipTests'
            }
        }
    }

    post {
        always {
            junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
            archiveArtifacts artifacts: '**/target/site/**/*.*', fingerprint: true
            archiveArtifacts artifacts: 'target/staging/**/*.*', fingerprint: true, allowEmptyArchive: true
            archiveArtifacts artifacts: '**/target/**/*.jar', fingerprint: true
            archiveArtifacts artifacts: '**/target/**/*.war', fingerprint: true
        }
    }
}
