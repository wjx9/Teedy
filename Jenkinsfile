pipeline {
    agent any

    options {
        timestamps()
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build Artifacts') {
            steps {
                sh 'mvn -DskipTests clean package'
            }
        }

        stage('Unit Tests') {
            steps {
                sh 'mvn -pl docs-web-common -am -Dtest=TestValidationUtil,TestPrincipal,TestRestException,TestCorsFilter -Dsurefire.failIfNoSpecifiedTests=false test'
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
                }
            }
        }

        stage('Site Documentation') {
            steps {
                sh 'mvn -DskipTests site site:stage'
            }
        }

        stage('Archive Artifacts') {
            steps {
                archiveArtifacts artifacts: '**/target/*.jar, **/target/*.war, target/staging/**', fingerprint: true
            }
        }
    }

    post {
        always {
            junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
        }
    }
}
