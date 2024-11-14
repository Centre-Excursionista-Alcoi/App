pipeline {
    agent any
    environment {
        DIR = 'App'
    }
    stages {
        stage('Build binaries') {
            stages {
                stage('Build for MacOS') {
                    agent {
                        label "macos"
                    }
                    environment {
                        APPLE_IDENTITY = credentials('APPLE_IDENTITY')
                        APPLE_ID = credentials('APPLE_ID')
                        NOTARIZATION_PASSWORD = credentials('NOTARIZATION_PASSWORD')
                        TEAM_ID = credentials('TEAM_ID')
                    }
                    stages {
                        stage('Prepare notarization credentials file') {
                            steps {
                                sh 'touch notarization.properties'
                                sh 'echo "APPLE_ID=$APPLE_ID" >> notarization.properties'
                                sh 'echo "NOTARIZATION_PASSWORD=$NOTARIZATION_PASSWORD" >> notarization.properties'
                                sh 'echo "TEAM_ID=$TEAM_ID" >> notarization.properties'
                            }
                        }
                        stage('Build Dmg') {
                            steps {
                                sh './gradlew --no-daemon :composeApp:notarizeDmg -Pcompose.desktop.mac.sign=true -Dorg.gradle.java.home=$JAVA_HOME_17'
                            }
                            post {
                                success {
                                    archiveArtifacts artifacts: 'composeApp/build/compose/**/*.dmg', fingerprint: true
                                }
                            }
                        }
                    }
                }
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
