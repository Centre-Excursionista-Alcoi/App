pipeline {
    agent any
    environment {
        DIR = 'App'
        GITHUB_API_TOKEN = credentials('GITHUB_TOKEN')
        GITHUB_REPO = 'Centre-Excursionista-Alcoi/App'
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
                        label "windows"
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
                stage('Build for MacOS') {
                    agent {
                        label "macos"
                    }
                    environment {
                        APPLE_IDENTITY = credentials('APPLE_IDENTITY')
                        APPLE_ID = credentials('APPLE_ID')
                        NOTARIZATION_PASSWORD = credentials('NOTARIZATION_PASSWORD')
                        TEAM_ID = credentials('TEAM_ID')
                        APPLE_KEYCHAIN_PASSWORD = credentials('APPLE_KEYCHAIN_PASSWORD')
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
                        stage('Unlock keychain') {
                            steps {
                                sh 'security unlock-keychain -p "$APPLE_KEYCHAIN_PASSWORD" ${HOME}/Library/Keychains/login.keychain-db'
                            }
                        }
                        stage('Build Dmg') {
                            steps {
                                sh './gradlew --no-daemon :composeApp:notarizeDmg -Pcompose.desktop.mac.sign=true -Dorg.gradle.java.home=$JAVA_HOME_17'
                            }
                            post {
                                success {
                                    archiveArtifacts artifacts: 'composeApp/build/compose/binaries/main/dmg/*.dmg', fingerprint: true
                                }
                            }
                        }
                        stage('Upload to GitHub Release') {
                            when {
                                expression { currentBuild.getRawBuild().getCause(hudson.triggers.SCMTrigger.SCMTriggerCause) == null } // Skip if triggered by SCM
                            }
                            steps {
                                script {
                                    // Parse release information from webhook payload
                                    def releasePayload = readJSON text: env.GITHUB_WEBHOOK_PAYLOAD
                                    def releaseTag = releasePayload.release.tag_name
                                    def releaseId = releasePayload.release.id

                                    // Get the artifact path
                                    def artifactPath = 'composeApp/build/compose/binaries/main/dmg/*.dmg' // Update this to match your artifact

                                    // Use GitHub API to upload the artifact
                                    sh """
                                        curl -X POST \
                                            -H "Authorization: token ${env.GITHUB_API_TOKEN}" \
                                            -H "Content-Type: application/zip" \
                                            --data-binary @${artifactPath} \
                                            "https://uploads.github.com/repos/${env.GITHUB_REPO}/releases/${releaseId}/assets?name=$(basename ${artifactPath})"
                                    """
                                }
                            }
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
