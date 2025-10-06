import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinxSerialization)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    
    iosArm64()
    iosSimulatorArm64()

    jvm()

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }
    
    sourceSets {
        commonMain.dependencies {
            // When adding dependencies here, also add them to Dockerfile in /server
            implementation(libs.kotlinx.serializationJson)
            api(libs.kotlinx.datetime)
            api(libs.kotlin.crypto.random)
            api(libs.kotlin.crypto.sha2)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }

    kotlin {
        compilerOptions {
            optIn.add("kotlin.uuid.ExperimentalUuidApi")
        }
    }
}

android {
    namespace = "org.centrexcursionistalcoi.app.shared"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
}
