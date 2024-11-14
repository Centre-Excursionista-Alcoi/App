pipeline {
    agent any
    environment {
        DIR = 'App'
    }
    stages {
        stage('Build for Linux') {
            steps {
                sh './gradlew --no-daemon :composeApp:packageDeb -Dorg.gradle.java.home=$JAVA_HOME_17'
            }
        }
    }
    post {
        success {
            archiveArtifacts artifacts: 'App/composeApp/build/outputs/**/*.apk', fingerprint: true
        }
        always {
            cleanWs()
        }
    }
}
