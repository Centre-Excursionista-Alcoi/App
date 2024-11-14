pipeline {
    agent any
    environment {
        DIR = 'App'
    }
    stages {
        stage('Build binaries') {
            parallel {
                stage('Build for Linux') {
                    agent {
                        label "linux"
                    }
                    steps {
                        sh './gradlew --no-daemon :composeApp:packageDeb -Dorg.gradle.java.home=$JAVA_HOME_17'
                    }
                    post {
                        success {
                            archiveArtifacts artifacts: 'composeApp/build/compose/**/*.deb', fingerprint: true
                        }
                    }
                }
                stage('Build for Windows') {
                    agent {
                        label "linux"
                    }
                    steps {
                        sh './gradlew --no-daemon :composeApp:packageExe -Dorg.gradle.java.home=$JAVA_HOME_17'
                    }
                    post {
                        success {
                            archiveArtifacts artifacts: 'composeApp/build/compose/**/*.exe', fingerprint: true
                        }
                    }
                }
            }
        }
    }
    post {
        always {
            cleanWs()
        }
    }
}
