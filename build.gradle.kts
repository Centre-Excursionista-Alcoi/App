import org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnRootExtension

plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidx.room3) apply false
    alias(libs.plugins.buildkonfig) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.googleServices) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinMultiplatformAndroid) apply false
    alias(libs.plugins.kotlinxSerialization) apply false
    alias(libs.plugins.kover) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.sentryAndroid) apply false
    alias(libs.plugins.sentryJvm) apply false
    alias(libs.plugins.sentryMultiplatform) apply false
}

// The wasmJs target's yarn.lock (kotlin-js-store/wasm/yarn.lock) transitively pulls in `ws`, which had a
// high-severity DoS advisory (GHSA-3h5v-q93c-6h6q) below 8.21.0. The Kotlin/Wasm tooling itself doesn't
// depend on a fixed version, so pin the resolution here instead of waiting for a Kotlin Gradle plugin bump.
// Regenerate the lockfile after changing this with `./gradlew kotlinWasmUpgradeYarnLock`.
rootProject.plugins.withType<WasmYarnPlugin> {
    rootProject.the<WasmYarnRootExtension>().resolution("ws", "8.21.0")
}