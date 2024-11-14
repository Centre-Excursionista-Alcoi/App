pipeline {
    agent any
    environment {
        GITHUB_TOKEN = credentials('GITHUB_TOKEN')
        DIR = 'App'
    }
    stages {
        stage('Checkout') {
            steps {
                sh 'git clone https://github.com/Centre-Excursionista-Alcoi/App.git'
            }
        }
        stage('Setup') {
            steps {
                dir(DIR) {
                    sh 'chmod +x ./gradlew'
                    sh 'echo sdk.dir=/android-sdk >> local.properties'
                }
            }
        }
        stage('Load credentials') {
            environment {
                KEYSTORE_PASSWORD = credentials('ANDROID_KEYSTORE_PASSWORD')

                KEY = credentials('ANDROID_KEY')
                KEY_ALIAS = '$KEY_USR'
                KEY_PASSWORD = '$KEY_PSW'

                KEYSTORE_PATH = credentials('ANDROID_KEYSTORE')
            }
            steps {
                dir(DIR) {
                    sh 'cp "$KEYSTORE_PATH" ./composeApp'
                }
            }
        }
        stage('Assemble and Bundle Release') {
            steps {
                dir(DIR) {
                    sh './gradlew :composeApp:assembleRelease'
                }
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