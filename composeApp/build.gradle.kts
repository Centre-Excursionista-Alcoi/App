import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.BOOLEAN
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.INT
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
import java.util.Properties
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.buildkonfig)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.serialization)
}

val appVersion = file("version.txt").readText()

fun readPropertiesFile(path: String): Properties? {
    val file = rootProject.file(path)
    if (!file.exists()) {
        return null
    }
    val props = Properties()
    file.inputStream().use { props.load(it) }
    return props
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    jvm("desktop")

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        moduleName = "composeApp"
        browser {
            val rootDirPath = project.rootDir.path
            val projectDirPath = project.projectDir.path
            commonWebpackConfig {
                outputFileName = "composeApp.js"
                devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
                    static = (static ?: mutableListOf()).apply {
                        // Serve sources to debug inside browser
                        add(rootDirPath)
                        add(projectDirPath)
                    }
                }
            }
        }
        binaries.executable()
    }

    sourceSets {
        val desktopMain by getting

        commonMain.dependencies {
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.runtime)
            implementation(compose.ui)

            implementation(libs.compose.carbon)
            implementation(libs.compose.coil.base)
            implementation(libs.compose.filekit)
            implementation(libs.compose.windowSizeClass)

            implementation(libs.compose.navigation)

            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)

            implementation(libs.kotlinx.datetime)

            implementation(libs.napier)

            // Ktor
            implementation(libs.ktor.client.auth)
            implementation(libs.ktor.client.contentNegotiation)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.serialization.kotlinxJson)

            // Multiplatform Settings
            implementation(libs.multiplatformSettings.base)
            implementation(libs.multiplatformSettings.coroutines)
            implementation(libs.multiplatformSettings.makeObservable)
            implementation(libs.multiplatformSettings.serialization)

            implementation(projects.shared)
        }
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.appcompat)
            implementation(libs.androidx.activity.compose)
            implementation(libs.kotlinx.coroutines.android)

            implementation(libs.datastore.base)
            implementation(libs.datastore.preferences)
            implementation(libs.multiplatformSettings.datastore)

            implementation(libs.ktor.client.okhttp)
        }
        iosMain.dependencies {
            implementation(libs.datastore.base)
            implementation(libs.datastore.preferences)
            implementation(libs.multiplatformSettings.datastore)
            implementation(libs.ktor.client.darwin)
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)

            implementation(libs.datastore.base)
            implementation(libs.datastore.preferences)
            implementation(libs.multiplatformSettings.datastore)

            implementation(libs.ktor.client.okhttp)
        }
    }
}

android {
    namespace = "org.centrexcursionistalcoi.app"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    androidResources {
        generateLocaleConfig = true
    }
    defaultConfig {
        applicationId = "org.centrexcursionistalcoi.app"

        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()

        versionCode = file("code.txt").readText().toInt()
        versionName = appVersion
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    signingConfigs {
        create("release") {
            storeFile = file("keystore.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

buildkonfig {
    packageName = "org.centrexcursionistalcoi.app"

    defaultConfigs {
        buildConfigField(STRING, "VERSION", appVersion)

        buildConfigField(STRING, "BACKEND_HOST", System.getenv("BACKEND_HOST") ?: "ceaapp.escalaralcoiaicomtat.org")
        buildConfigField(INT, "BACKEND_PORT", System.getenv("BACKEND_PORT") ?: "443")
        buildConfigField(BOOLEAN, "BACKEND_HTTPS", System.getenv("BACKEND_HTTPS") ?: "true")
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}

compose.desktop {
    application {
        mainClass = "org.centrexcursionistalcoi.app.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Exe, TargetFormat.Deb)
            packageName = "org.centrexcursionistalcoi.app"
            packageVersion = appVersion

            description = "The official app for the Centre Excursionista d'Alcoi"
            copyright = "© 2024 Arnau Mora Gras. All rights reserved."
            vendor = "Centre Excursionista d'Alcoi"
            licenseFile.set(rootProject.file("LICENSE"))

            val icons = file("icons")

            linux {
                debPackageVersion = appVersion

                debMaintainer = "arnyminerz@proton.me"
                menuGroup = "Centre Excursionista d'Alcoi"
                appCategory = "misc"

                iconFile.set(File(icons, "CEA.png"))

                // Required by Filekit: https://github.com/vinceglb/FileKit?tab=readme-ov-file#-installation
                modules("jdk.security.auth")
            }
            macOS {
                bundleID = "org.centrexcursionistalcoi.app"
                dockName = "CEA App"
                packageName = "CEA App"
                appStore = false
                appCategory = "public.app-category.utilities"

                iconFile.set(File(icons, "CEA.icns"))

                signing {
                    identity.set(System.getenv("APPLE_IDENTITY"))

                    val keychainPath = System.getenv("APPLE_KEYCHAIN_PATH")
                    if (keychainPath != null) {
                        keychain.set(keychainPath)
                    }
                }

                val notarizationProps = readPropertiesFile("notarization.properties")
                if (notarizationProps != null) {
                    notarization {
                        appleID.set(notarizationProps["APPLE_ID"] as String)
                        password.set(notarizationProps["NOTARIZATION_PASSWORD"] as String)
                        teamID.set(notarizationProps["TEAM_ID"] as String)
                    }
                }
            }
            windows {
                menuGroup = "Centre Excursionista d'Alcoi"
                upgradeUuid = "c6d0ef80-3e17-4fed-b4e1-92b0161373b4"

                exePackageVersion = appVersion

                iconFile.set(File(icons, "CEA.ico"))
            }
        }
    }
}
