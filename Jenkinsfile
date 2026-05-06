pipeline {
    agent any

    environment {
        JAVA_HOME = "/Library/Java/JavaVirtualMachines/amazon-corretto-11.jdk/Contents/Home"
        PATH = "/Library/Java/JavaVirtualMachines/amazon-corretto-11.jdk/Contents/Home/bin:/opt/homebrew/bin:/usr/local/bin:${env.PATH}"
        MAVEN_ARGS = "-B -ntp"
        DOCS_DEFAULT_LANGUAGE = "en"
        DOCS_BASE_URL = "http://localhost/docs"
    }

    options {
        disableConcurrentBuilds()
        timestamps()
    }

    stages {
        stage('Clean') {
            steps {
                sh 'mvn ${MAVEN_ARGS} clean'
            }
        }

        stage('Compile') {
            steps {
                sh 'mvn ${MAVEN_ARGS} compile'
            }
        }

        stage('Test') {
            steps {
                sh 'mvn ${MAVEN_ARGS} test -Dmaven.test.failure.ignore=true'
            }
        }

        stage('PMD') {
            steps {
                sh 'mvn ${MAVEN_ARGS} install -DskipTests pmd:pmd'
            }
        }

        stage('JaCoCo') {
            steps {
                sh 'mvn ${MAVEN_ARGS} jacoco:report'
            }
        }

        stage('Javadoc') {
            steps {
                sh 'mvn ${MAVEN_ARGS} javadoc:javadoc'
            }
        }

        stage('Site') {
            steps {
                sh 'mvn ${MAVEN_ARGS} site'
            }
        }

        stage('Package') {
            steps {
                sh 'mvn ${MAVEN_ARGS} package -DskipTests'
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
