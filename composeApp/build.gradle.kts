import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.BOOLEAN
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
import java.util.Properties
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.buildkonfig)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.googleServices)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.sentryAndroid)
    alias(libs.plugins.sentryMultiplatform)
    alias(libs.plugins.sqldelight)
}

fun readProperties(fileName: String): Properties? {
    val propsFile = project.file(fileName)
    if (!propsFile.exists()) {
        return null
    }
    if (!propsFile.canRead()) {
        throw GradleException("Cannot read $fileName")
    }
    return Properties().apply {
        propsFile.inputStream().use { load(it) }
    }
}

val versionProperties = readProperties("version.properties")!!

val appVersionName: String = versionProperties.getProperty("VERSION_NAME")
val appVersionCode: String = versionProperties.getProperty("VERSION_CODE")

val credentialsProperties = readProperties("credentials.properties")
fun getCredential(key: String): String? {
    return System.getenv(key) ?: credentialsProperties?.getProperty(key)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }
    
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
            linkerOpts += "-lsqlite3"
            export(libs.sentry.kotlinMultiplatform)
            export(libs.kmm.notifier)
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
            implementation(compose.material3)
            implementation(libs.compose.windowSizeClass)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)

            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            // Compose Navigation
            implementation(libs.androidx.navigation.compose)

            // Ktor serialization
            implementation(libs.ktor.serialization.kotlinxJson)

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

            // File picker dialogs
            implementation(libs.filekit.coil)
            implementation(libs.filekit.dialogs.compose)

            implementation(libs.coil.compose)

            // QR Code Generation
            implementation(libs.qrose.oned)
            implementation(libs.qrose.twod)

            // QR Code Scanner
            implementation(libs.kscan)

            // Runtime permission management
            implementation(libs.kmm.permission)

            // Push Notifications
            implementation(libs.kmm.notifier)

            // SQLDelight extensions
            implementation(libs.sqldelight.adapters)
            implementation(libs.sqldelight.coroutines)

            implementation(projects.shared)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
        }

        val phonesMain by creating {
            dependsOn(commonMain.get())
            dependencies {
                implementation(libs.kotlinx.io.core)
            }
        }

        androidMain {
            dependsOn(phonesMain)
            dependencies {
                implementation(compose.preview)
                implementation(libs.androidx.activity.compose)
                implementation(libs.ktor.client.android)

                // Custom Tabs support
                implementation(libs.androidx.browser)

                implementation(libs.sqldelight.android)

                // WorkManager
                implementation(libs.bundles.androidx.work)

                // Reflection support
                implementation(kotlin("reflect"))
            }
        }

        iosMain {
            dependsOn(phonesMain)
            dependencies {
                implementation(libs.ktor.client.darwin)
                implementation(libs.sqldelight.native)
            }
        }
        iosArm64Main { dependsOn(iosMain.get()) }
        iosSimulatorArm64Main { dependsOn(iosMain.get()) }

        webMain { dependsOn(commonMain.get()) }
        wasmJsMain {
            dependsOn(webMain.get())
            dependencies {
                implementation(libs.ktor.client.js)
                implementation(libs.kotlinx.browser)

                // SQLDelight for WASM
                implementation(libs.sqldelight.wasm)
                implementation(npm("@cashapp/sqldelight-sqljs-worker", "2.0.2"))
                implementation(npm("sql.js", "1.6.2"))
                implementation(devNpm("copy-webpack-plugin", "9.1.0"))
            }
        }
        wasmJsTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
            }
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xcontext-parameters")
        freeCompilerArgs.add("-Xexpect-actual-classes")
        optIn.add("kotlin.time.ExperimentalTime")
        optIn.add("kotlin.uuid.ExperimentalUuidApi")
    }
}

sqldelight {
    databases {
        create("Database") {
            generateAsync.set(true)
            packageName.set("org.centrexcursionistalcoi.app.database")
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
        versionName = appVersionName
        versionCode = appVersionCode.toInt()
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    signingConfigs {
        create("release") {
            keyAlias = getCredential("KEYSTORE_ALIAS")
            keyPassword = getCredential("KEYSTORE_ALIAS_PASSWORD")

            storeFile = File(rootDir, "keystore.jks")
            storePassword = getCredential("KEYSTORE_PASSWORD")
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            isShrinkResources = false

            proguardFiles(
                // Default file with automatically generated optimization rules.
                getDefaultProguardFile("proguard-android-optimize.txt"),
            )

            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

val sentryProperties = readProperties("sentry.properties")!!

buildkonfig {
    packageName = "org.centrexcursionistalcoi.app"

    defaultConfigs {
        buildConfigField(
            type = STRING,
            name = "SERVER_URL",
            value = "https://server.cea.arnaumora.com",
        )
        buildConfigField(
            type = STRING,
            name = "REDIRECT_URI",
            value = null,
            nullable = true,
        )
        buildConfigField(
            type = STRING,
            name = "SENTRY_DSN",
            value = null,
            nullable = true,
        )
        buildConfigField(
            type = BOOLEAN,
            name = "DEBUG",
            value = (System.getenv("PRODUCTION") != "true").toString(),
        )
        buildConfigField(
            type = STRING,
            name = "VERSION_NAME",
            value = appVersionName,
        )
        buildConfigField(
            type = STRING,
            name = "VERSION_CODE",
            value = appVersionCode,
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
            buildConfigField(
                type = STRING,
                name = "SENTRY_DSN",
                value = sentryProperties.getProperty("SENTRY_DSN_ANDROID"),
                nullable = true,
            )
        }
        create("wasmJs") {
            buildConfigField(
                type = STRING,
                name = "REDIRECT_URI",
                value = "http://localhost:8080#redirect",
                nullable = true,
            )
        }
        create("ios") {
            buildConfigField(
                type = STRING,
                name = "SENTRY_DSN",
                value = sentryProperties.getProperty("SENTRY_DSN_IOS"),
                nullable = true,
            )
        }
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}

sentry {
    // Prevent Sentry dependencies from being included in the Android app through the AGP.
    autoInstallation {
        enabled.set(false)
    }

    // The slug of the Sentry organization to use for uploading proguard mappings/source contexts.
    org.set("centre-excursionista-alcoi")
    // The slug of the Sentry project to use for uploading proguard mappings/source contexts.
    projectName.set("app-android")
    // The authentication token to use for uploading proguard mappings/source contexts.
    // WARNING: Do not expose this token in your build.gradle files, but rather set an environment
    // variable and read it into this property.
    authToken.set(getCredential("SENTRY_AUTH_TOKEN"))
}
