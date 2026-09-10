import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.kilua)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinxSerialization)
}

@OptIn(ExperimentalWasmDsl::class)
kotlin {
    // Only a wasmJs target -- :shared (whose DTOs this module's admin API calls need) only publishes a wasmJs
    // browser target, not a plain js one, so a js target here could never link against it anyway. wasmJs-only
    // is a normal, well-supported Kilua deployment shape and fine for an internal, admin-only panel.
    wasmJs {
        useEsModules()
        browser {
            commonWebpackConfig {
                cssSupport {
                    enabled = true
                }
                outputFileName = "main.bundle.js"
                sourceMaps = false
            }
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }
        binaries.executable()
        compilerOptions {
            target.set("es2015")
        }
    }
    sourceSets {
        getByName("commonMain") {
            dependencies {
                implementation(projects.shared)
                implementation(libs.kilua)
                implementation(libs.kilua.bootstrap)
                implementation(libs.kilua.rest)
                implementation(libs.kilua.routing)
                implementation(libs.kilua.tabulator)
            }
        }
    }
}
