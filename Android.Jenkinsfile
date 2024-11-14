pipeline {
    agent any
    environment {
        DIR = 'App'
    }
    stages {
        stage('Setup') {
            steps {
                sh 'echo sdk.dir=/android-sdk >> local.properties'
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
                sh 'cp "$KEYSTORE_PATH" ./composeApp'
            }
        }
        stage('Assemble and Bundle Release') {
            steps {
                sh './gradlew :composeApp:assembleRelease'
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
