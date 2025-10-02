import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.buildkonfig)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.sekret)
    alias(libs.plugins.sqldelight)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }
    
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }
    
    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(libs.compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)

            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            // Compose Navigation
            implementation(libs.androidx.navigation.compose)

            // Ktor client
            implementation(libs.ktor.client.auth)
            implementation(libs.ktor.client.contentNegotiation)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.logging)

            // Settings
            implementation(libs.kmm.settings.core)
            implementation(libs.kmm.settings.coroutines)
            implementation(libs.kmm.settings.makeObservable)
            implementation(libs.kmm.settings.noArg)
            implementation(libs.kmm.settings.serialization)

            // Logging
            implementation(libs.napier)

            implementation(libs.kotlinx.serializationJson)

            implementation(libs.sqldelight.coroutines)

            implementation(projects.shared)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }

        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.client.android)

            // Custom Tabs support
            implementation(libs.androidx.browser)

            implementation(libs.sqldelight.android)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.sqldelight.native)
        }

        wasmJsMain.dependencies {
            implementation(libs.ktor.client.js)

            // SQLDelight for WASM
            implementation(libs.sqldelight.wasm)
            implementation(devNpm("copy-webpack-plugin", "9.1.0"))
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}

sqldelight {
    databases {
        create("Database") {
            packageName.set("org.centrexcursionistalcoi.app.database")
            generateAsync.set(true)
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
        versionCode = 2_00_00_000
        versionName = "2.0.0"

        // Provide redirect URI for OIDC Auth flow
        addManifestPlaceholders(
            mapOf("oidcRedirectScheme" to "cea")
        )
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

sekret {
    properties {
        enabled.set(true)

        packageName.set("org.centrexcursionistalcoi.app")
        encryptionKey.set(System.getenv("SEKRET_KEY") ?: "0123456789ABCDEF") // Use a secure key in production!
    }
}

buildkonfig {
    packageName = "org.centrexcursionistalcoi.app"

    defaultConfigs {
        buildConfigField(
            type = STRING,
            name = "SERVER_URL",
            value = "https://server.cea.arnaumora.com",
            const = true,
        )
        buildConfigField(
            type = STRING,
            name = "REDIRECT_URI",
            value = null,
            nullable = true,
        )
    }
    targetConfigs {
        create("android") {
            buildConfigField(
                type = STRING,
                name = "REDIRECT_URI",
                value = "cea://redirect",
                nullable = true,
            )
        }
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}
