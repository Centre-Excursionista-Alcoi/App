import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatformAndroid)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinxSerialization)
}

kotlin {
    android {
        namespace = "org.centrexcursionistalcoi.app.shared"
        compileSdk {
            version = release(libs.versions.android.compileSdk.get().toInt()) {
                minorApiLevel = 0
            }
        }
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
        wasmJsMain.dependencies {
            // kotlinx-datetime has no bundled IANA time zone database on Wasm/JS: named zones (e.g. "Europe/Madrid")
            // are resolved through this npm package instead. See JsJodaTimeZone.kt for the required init hook.
            // Pinned to 2.23.0 (not latest): 2.24.0+ requires @js-joda/core >=5.7.0 as a peer, but kotlinx-datetime
            // 0.8.0 bundles @js-joda/core 3.2.0 -- 2.23.0 is the last release still compatible with that (>=1.11.0).
            implementation(npm("@js-joda/timezone", "2.23.0"))
        }
    }

    kotlin {
        compilerOptions {
            optIn.add("kotlin.uuid.ExperimentalUuidApi")
            optIn.add("kotlin.time.ExperimentalTime")
        }
    }
}
