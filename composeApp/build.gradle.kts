import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.BOOLEAN
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeSimulatorTest
import java.util.Calendar
import java.util.Properties

plugins {
    alias(libs.plugins.androidx.room3)
    alias(libs.plugins.buildkonfig)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinMultiplatformAndroid)
    alias(libs.plugins.kotlinxSerialization)
    // TODO: re-enable once Koin supports Kotlin 2.4.20+ -- crashes with an IrGenerationExtensionException
    //  (IrUtilsKt.getValueArgument signature changed). Manual replacement for what this plugin generated
    //  lives in di/ManualModules.kt. See https://github.com/Centre-Excursionista-Alcoi/App/issues/590
    //  and https://github.com/InsertKoinIO/koin-compiler-plugin/issues/89
    // alias(libs.plugins.koinCompilerPlugin)
    alias(libs.plugins.ksp)
    alias(libs.plugins.sentryMultiplatform)
}

fun readProperties(fileName: String, root: File = projectDir): Properties? {
    val propsFile = File(root, fileName)
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

val versionProperties = readProperties("version.properties", rootDir)!!

val appVersionName: String = versionProperties.getProperty("VERSION_NAME")
val appVersionCode: String = versionProperties.getProperty("VERSION_CODE")

kotlin {
    android {
        namespace = "org.centrexcursionistalcoi.app.android"
        compileSdk {
            version = release(libs.versions.android.compileSdk.get().toInt()) {
                minorApiLevel = 0
            }
        }
        minSdk = libs.versions.android.minSdk.get().toInt()

        androidResources {
            enable = true
        }

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }
    
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { target ->
        target.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
            // Required when using NativeSQLiteDriver
            linkerOpts.add("-lsqlite3")
            export(libs.sentry.kotlinMultiplatform)
            export(libs.kmm.notifier.core)
            export(libs.kmm.notifier.firebase)
        }
    }

    // Simulator Keychain access requires an application identity embedded in the test executable.
    // https://youtrack.jetbrains.com/issue/KT-61470
    iosSimulatorArm64 {
        val keychainTestEntitlements = project.file("src/iosTest/KeychainTests.entitlements")
        binaries.getTest("DEBUG").apply {
            linkerOpts("-sectcreate", "__TEXT", "__entitlements", keychainTestEntitlements.absolutePath)
            linkTaskProvider.configure { inputs.file(keychainTestEntitlements) }
        }
        tasks.withType<KotlinNativeSimulatorTest>().configureEach {
            // Standalone simctl execution has no Keychain service (errSecNotAvailable).
            standalone.set(false)
            device.set("booted") // Override with --device <simulator UUID> if several are booted.
        }
    }

    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.windowSizeClass)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.components.uiToolingPreview)

            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            // Compose Navigation
            implementation(libs.androidx.navigation3.ui)

            // Runtime Language Change
            implementation(libs.localina)

            // Rich Text Editor
            implementation(libs.richeditor)

            // Calendar Viewer
            implementation(libs.calendar)

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
            implementation(libs.logging)

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

            // For rendering Markdown
            implementation(libs.bundles.markdownRenderer)

            // Zoomable images
            implementation(libs.zoomable)

            // Runtime permission management
            // FIXME: Currently not implemented for Desktop: https://github.com/reyazoct/Kmm-Permissions/issues/2
            // implementation(libs.kmm.permission)

            // Push Notifications (must be API for exporting to iOS)
            // kmm-notifier-core is only a transitive dependency of kmm-notifier-firebase, but Kotlin/Native's
            // export() only flattens the ObjC/Swift naming (e.g. "KMPNotifier" instead of a module-qualified
            // "ComposeAppKmpnotifier_coreKMPNotifier") for dependencies exported directly -- it must be declared
            // and exported here too, or Swift call sites referencing bare "KMPNotifier" won't compile.
            api(libs.kmm.notifier.core)
            api(libs.kmm.notifier.firebase)

            // Room 3
            implementation(libs.androidx.room3.runtime)
            implementation(libs.androidx.sqlite.bundled)

            // Koin dependency injection
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.koin.ktor)
            api(libs.koin.annotations)

            api(projects.shared)
        }
        named("commonMain").configure {
            kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
            implementation(libs.koin.test)
        }

        // Platforms that require granting permissions
        val permissionsMain = create("permissionsMain") {
            dependsOn(commonMain.get())
            dependencies {
                implementation(libs.kmm.permission)
            }
        }

        // Implements workers with Kotlin Coroutines
        // Includes: jvm, iOS
        val coroutinesWorkersMain = create("coroutinesWorkersMain") {
            dependsOn(commonMain.get())
        }

        jvmMain {
            dependsOn(coroutinesWorkersMain)
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlinx.coroutines.swing)
                implementation(libs.ktor.client.java)
            }
        }

        jvmTest.dependencies {
            implementation(libs.mockk)
        }

        val phonesMain = create("phonesMain") {
            dependsOn(permissionsMain)
        }

        androidMain {
            dependsOn(phonesMain)
            dependencies {
                implementation(libs.androidx.appcompat)
                implementation(libs.androidx.activity.compose)
                implementation(libs.ktor.client.android)

                // Custom Tabs support
                implementation(libs.androidx.browser)

                // WorkManager
                implementation(libs.bundles.androidx.work)

                // Reflection support
                implementation(kotlin("reflect"))

                // In-App Update check
                implementation(libs.android.appUpdate)

                // Room3 SQLite Wrapper
                implementation(libs.androidx.room3.sqliteWrapper)

                // Koin Extensions for Android
                implementation(libs.koin.android)
                implementation(libs.koin.androidx.workmanager)
            }
        }

        iosMain {
            dependsOn(phonesMain)
            dependsOn(coroutinesWorkersMain)
            dependencies {
                implementation(libs.ktor.client.darwin)
            }
        }
        iosArm64Main { dependsOn(iosMain.get()) }
        iosSimulatorArm64Main { dependsOn(iosMain.get()) }

        // The default hierarchy template is disabled, so wire the shared iOS tests explicitly.
        iosTest {
            dependsOn(commonTest.get())
        }
        iosArm64Test { dependsOn(iosTest.get()) }
        iosSimulatorArm64Test { dependsOn(iosTest.get()) }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
        optIn.add("kotlin.time.ExperimentalTime")
        optIn.add("kotlin.uuid.ExperimentalUuidApi")
    }

}

dependencies {
    listOf(
        libs.androidx.room3.compiler,
    ).forEach { dependency ->
        add("kspAndroid", dependency)
        add("kspIosSimulatorArm64", dependency)
        add("kspIosArm64", dependency)
        add("kspJvm", dependency)
    }
}

// Trigger Common Metadata Generation from Native tasks
tasks.matching { it.name.startsWith("ksp") && it.name != "kspCommonMainKotlinMetadata" }.configureEach {
    dependsOn("kspCommonMainKotlinMetadata")
}

// TODO: re-enable alongside the `koinCompilerPlugin` alias above (see issue link there).
// koinCompiler {
//     // The plugin's compile-time graph verification (auto-enabled once it detects startKoin/@KoinApplication)
//     // misfires on this project as a false positive, reporting @Singleton/@ComponentScan-provided classes as
//     // missing even though they resolve correctly at runtime. Disable it until upstream fixes the detector.
//     compileSafety = false
// }

room3 {
    schemaDirectory("$projectDir/schemas")
}

compose.desktop {
    application {
        mainClass = "org.centrexcursionistalcoi.app.MainKt"

        buildTypes.release.proguard {
            isEnabled = false
        }

        nativeDistributions {
            targetFormats(TargetFormat.Exe, TargetFormat.Deb, TargetFormat.Dmg)

            packageName = "org.centrexcursionistalcoi.app"
            packageVersion = appVersionName

            description = "CEA App"
            val year = Calendar.getInstance().get(Calendar.YEAR)
            copyright = "© $year Centre Excursionista d'Alcoi. © $year Arnau Mora Gras. All rights reserved."
            vendor = "Centre Excursionista d'Alcoi"

            // Add additional modules that are required for your application
            modules("java.net.http", "java.sql")

            val iconsDir = File(projectDir, "icons")

            windows {
                iconFile.set(
                    File(iconsDir, "icon.ico")
                )
                dirChooser = true
                perUserInstall = true
                menuGroup = "CEA App"
                packageName = "CEA App"
                upgradeUuid = "5504905b-d0a6-44a6-acb7-5ddcfbaa4ef8"
                msiPackageVersion = appVersionName
                exePackageVersion = appVersionName
            }
            linux {
                iconFile.set(
                    File(iconsDir, "icon.png")
                )
                debMaintainer = "app.linux.cea@arnyminerz.com"
                menuGroup = "CEA App"
                appCategory = "Sports"
                appRelease = appVersionCode
                debPackageVersion = appVersionName
                rpmPackageVersion = appVersionName
            }
            macOS {
                iconFile.set(
                    File(iconsDir, "icon.icns")
                )
                bundleID = "org.centrexcursionistalcoi.app"
                packageName = "CEA App"
                packageVersion = appVersionName
                dockName = "CEA App"

                // Signing/notarization identity is only present on CI (or when a developer
                // opts in locally); without it, unsigned dev builds still work as before.
                fun env(name: String) = System.getenv(name)?.takeIf { it.isNotBlank() }

                val signingIdentity = env("MACOS_SIGNING_IDENTITY")
                signing {
                    sign.set(signingIdentity != null)
                    signingIdentity?.let { identity.set(it) }
                }
                notarization {
                    env("NOTARIZATION_APPLE_ID")?.let { appleID.set(it) }
                    env("NOTARIZATION_PASSWORD")?.let { password.set(it) }
                    env("NOTARIZATION_TEAM_ID")?.let { teamID.set(it) }
                }
            }
        }
    }
}

afterEvaluate {
    tasks.withType<JavaExec> {
        jvmArgs("--add-opens", "java.desktop/sun.awt=ALL-UNNAMED")
        jvmArgs("--add-opens", "java.desktop/java.awt.peer=ALL-UNNAMED")

        if (System.getProperty("os.name").contains("Mac")) {
            jvmArgs("--add-opens", "java.desktop/sun.awt=ALL-UNNAMED")
            jvmArgs("--add-opens", "java.desktop/sun.lwawt=ALL-UNNAMED")
            jvmArgs("--add-opens", "java.desktop/sun.lwawt.macosx=ALL-UNNAMED")
        }
    }
}

val sentryProperties = readProperties("sentry.properties")!!

buildkonfig {
    packageName = "org.centrexcursionistalcoi.app"

    defaultConfigs {
        buildConfigField(
            type = STRING,
            name = "SERVER_URL",
            value = System.getenv("SERVER_URL") ?: "https://server.centrexcursionistalcoi.app",
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
                name = "SENTRY_DSN",
                value = sentryProperties.getProperty("SENTRY_DSN_ANDROID"),
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
        create("jvm") {
            buildConfigField(
                type = STRING,
                name = "SENTRY_DSN",
                value = sentryProperties.getProperty("SENTRY_DSN_DESKTOP"),
                nullable = true,
            )
        }
    }
}

configurations.configureEach {
    exclude(group = "org.jetbrains.compose.material", module = "material")
}

// Gate Apple-only configuration behind an actual Apple build being requested: anything in here that runs
// an external process during Gradle's configuration phase would otherwise make the configuration cache
// fail on every invocation, including unrelated ones.
val isBuildingAppleTarget = gradle.startParameter.taskNames.any {
    it.contains("Ios", ignoreCase = true) || it.contains("Apple", ignoreCase = true)
}
if (isBuildingAppleTarget) sentryKmp {
    autoInstall {
        linker {
            // Sentry Cocoa is now consumed via the SwiftPM reference on the iosApp target
            // (see project.pbxproj) rather than CocoaPods. Without an explicit path, the
            // plugin walks the filesystem for *any* .xcodeproj to read its build settings from,
            // which can find an unrelated one belonging to a transitive SPM dependency instead
            // of ours -- pointing it here directly avoids that.
            xcodeprojPath = rootDir.resolve("iosApp/iosApp.xcodeproj").absolutePath

            // The plugin's own search strategies only ever look under the default
            // ~/Library/Developer/Xcode/DerivedData, so they never find anything on a machine
            // where Xcode's DerivedData location has been redirected elsewhere
            // (Xcode > Settings > Locations > Derived Data, i.e. IDECustomDerivedDataLocation).
            // Resolve the framework ourselves under whatever location is actually configured.
            // Best-effort and macOS-only: never break non-Apple builds (JVM/Android tests, etc.)
            // if this can't be determined.
            frameworkPath = runCatching {
                // Uses providers.exec (not a raw ProcessBuilder) specifically because this runs during Gradle's
                // configuration phase: the configuration cache tracks this as a proper input instead of
                // rejecting it as an untracked external process.
                val customLocation = providers.exec {
                    commandLine("defaults", "read", "com.apple.dt.Xcode", "IDECustomDerivedDataLocation")
                    isIgnoreExitValue = true
                }.standardOutput.asText.get().trim().takeIf { it.isNotBlank() }
                val derivedDataRoot = File(
                    customLocation ?: "${System.getProperty("user.home")}/Library/Developer/Xcode/DerivedData"
                )
                // Xcode can have more than one "iosApp-<hash>" folder at once (e.g. a
                // project-level xcodebuild invocation used for diagnostics gets its own
                // bucket, separate from the real workspace build) -- picking the most
                // recently touched one is unreliable, so check each for the actual file
                // instead, preferring the most recently modified one that has it.
                derivedDataRoot
                    .listFiles { file -> file.isDirectory && file.name.startsWith("iosApp-") }
                    ?.sortedByDescending { it.lastModified() }
                    ?.asSequence()
                    ?.map { it.resolve("SourcePackages/artifacts/sentry-cocoa/Sentry/Sentry.xcframework") }
                    ?.firstOrNull { it.exists() }
                    ?.absolutePath
            }.getOrNull()
        }
    }
}
