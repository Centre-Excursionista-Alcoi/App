import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatformAndroid)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinxSerialization)
}

kotlin {
    androidLibrary {
        namespace = "org.centrexcursionistalcoi.app.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
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
            api(libs.ktor.client.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }

    kotlin {
        compilerOptions {
            optIn.add("kotlin.uuid.ExperimentalUuidApi")
            optIn.add("kotlin.time.ExperimentalTime")
        }
    }
}
