import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.BOOLEAN
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.INT
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
import com.codingfeline.buildkonfig.gradle.TargetConfigDsl
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

val appVersion = "1.0.0"

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
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

    defaultConfig {
        applicationId = "org.centrexcursionistalcoi.app"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = appVersion
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

buildkonfig {
    packageName = "org.centrexcursionistalcoi.app"

    defaultConfigs {
        buildConfigField(STRING, "BACKEND_HOST", System.getenv("BACKEND_HOST") ?: "127.0.0.1")
        buildConfigField(INT, "BACKEND_PORT", System.getenv("BACKEND_PORT") ?: "8080")
        buildConfigField(BOOLEAN, "BACKEND_HTTPS", System.getenv("BACKEND_HTTPS") ?: "false")
    }

    targetConfigs {
        val productionBackend: TargetConfigDsl.() -> Unit = {
            buildConfigField(STRING, "BACKEND_HOST", "ceaapp.escalaralcoiaicomtat.org")
            buildConfigField(INT, "BACKEND_PORT", "443")
            buildConfigField(BOOLEAN, "BACKEND_HTTPS", "true")
        }

        create("android") {
            productionBackend()
        }
        create("ios") {
            productionBackend()
        }
        create("wasmJs") {
            productionBackend()
        }
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}

compose.desktop {
    application {
        mainClass = "org.centrexcursionistalcoi.app.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "org.centrexcursionistalcoi.app"
            packageVersion = appVersion

            // Required by Filekit: https://github.com/vinceglb/FileKit?tab=readme-ov-file#-installation
            linux {
                modules("jdk.security.auth")
            }
        }
    }
}
