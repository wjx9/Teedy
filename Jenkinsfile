pipeline {
    agent any

    environment {
        PATH = "/opt/homebrew/bin:/usr/local/bin:${env.PATH}"
        MAVEN_ARGS = "-B -ntp -Dtest=TestCoverageUtil,TestValidationUtil,TestPrincipal,TestRestException,TestCorsFilter -Dsurefire.failIfNoSpecifiedTests=false -DfailIfNoTests=false"
    }

    options {
        skipDefaultCheckout()
        disableConcurrentBuilds()
        timestamps()
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Verify Tools') {
            steps {
                sh 'command -v mvn'
                sh 'mvn -version'
            }
        }

        stage('Build, Test, Reports') {
            steps {
                sh 'mvn ${MAVEN_ARGS} clean verify site'
            }
        }
    }

    post {
        always {
            junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
            archiveArtifacts artifacts: '**/target/site/**/*.*', fingerprint: true, allowEmptyArchive: true
            archiveArtifacts artifacts: '**/target/**/*.jar', fingerprint: true, allowEmptyArchive: true
            archiveArtifacts artifacts: '**/target/**/*.war', fingerprint: true, allowEmptyArchive: true
        }
    }
}
