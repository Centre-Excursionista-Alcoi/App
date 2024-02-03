import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.buildkonfig)
    alias(libs.plugins.detekt)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.moko.resources)
    alias(libs.plugins.serialization)
}

fun readProperties(fileName: String): Properties {
    val propsFile = project.rootProject.file(fileName)
    if (!propsFile.exists()) {
        throw GradleException("$fileName doesn't exist")
    }
    if (!propsFile.canRead()) {
        throw GradleException("Cannot read $fileName")
    }
    return Properties().apply {
        propsFile.inputStream().use { load(it) }
    }
}

inline fun updateProperties(fileName: String, block: Properties.() -> Unit) {
    val propsFile = project.rootProject.file(fileName)
    val props = readProperties(propsFile.name)
    block(props)
    propsFile.outputStream().use {
        val date = LocalDateTime.now()
        props.store(it, "Updated at $date")
    }
}

open class PlatformVersion(
    open val versionName: String
)

data class PlatformVersionWithCode(
    override val versionName: String,
    val versionCode: Int
) : PlatformVersion(versionName)

typealias AndroidVersion = PlatformVersionWithCode
typealias IOSVersion = PlatformVersion
typealias WindowsVersion = PlatformVersion
typealias MacOSVersion = PlatformVersion
typealias LinuxVersion = PlatformVersionWithCode

sealed class Platform<VersionType : PlatformVersion> {
    object Android : Platform<PlatformVersionWithCode>()
    object IOS : Platform<IOSVersion>()
    object Windows : Platform<WindowsVersion>()
    object MacOS : Platform<MacOSVersion>()
    object Linux : Platform<LinuxVersion>()
}

fun <VersionType : PlatformVersion> getVersionForPlatform(platform: Platform<VersionType>?): VersionType {
    val versionProperties = readProperties("version.properties")

    fun getAndReplaceVersion(key: String): String {
        val versionName = versionProperties.getProperty("VERSION_NAME")
        return versionProperties.getProperty(key).replace("\$VERSION_NAME", versionName)
    }

    @Suppress("UNCHECKED_CAST")
    return when (platform) {
        Platform.Android -> PlatformVersionWithCode(
            getAndReplaceVersion("VERSION_ANDROID"),
            versionProperties.getProperty("VERSION_ANDROID_CODE").toInt()
        ) as VersionType

        Platform.IOS -> IOSVersion(getAndReplaceVersion("VERSION_NAME")) as VersionType
        Platform.Windows -> WindowsVersion(getAndReplaceVersion("VERSION_WIN")) as VersionType
        Platform.MacOS -> MacOSVersion(getAndReplaceVersion("VERSION_MAC")) as VersionType
        Platform.Linux -> LinuxVersion(
            getAndReplaceVersion("VERSION_LIN"),
            versionProperties.getProperty("VERSION_LIN_RELEASE").toInt()
        ) as VersionType

        else -> PlatformVersion(versionProperties.getProperty("VERSION_NAME")) as VersionType
    }
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
    }

    jvm("desktop")

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

    targets.all {
        compilations.all {
            compilerOptions.configure {
                freeCompilerArgs.add("-Xexpect-actual-classes")
            }
        }
    }

    @Suppress("UnusedPrivateProperty")
    sourceSets {
        val commonMain by getting {
            dependencies {
                // Jetpack Compose
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)
                implementation(compose.runtime)
                implementation(compose.ui)
                implementation(libs.compose.placeholder)

                // Compose - Voyager
                implementation(libs.voyager.navigator)
                implementation(libs.voyager.screenModel)

                // Compose - WindowSizeClass
                implementation(libs.compose.windowSizeClass)

                // Moko Resources
                implementation(libs.moko.resources.base)
                implementation(libs.moko.resources.compose)

                // Supabase
                implementation(libs.supabase.auth)
                implementation(libs.supabase.postgrest)
                implementation(libs.supabase.realtime)

                // Multiplatform Settings
                implementation(libs.multiplatformSettings.base)
                implementation(libs.multiplatformSettings.coroutines)
                implementation(libs.multiplatformSettings.serialization)

                // Logging library
                implementation(libs.napier)
            }
        }

        val androidMain by getting {
            dependsOn(commonMain)

            dependencies {
                implementation(libs.androidx.activity.compose)
                implementation(libs.compose.ui.tooling.preview)
                implementation(libs.ktor.client.android)

                // Multiplatform Settings
                implementation(libs.datastore)
                implementation(libs.multiplatformSettings.datastore)
            }
        }

        val desktopMain by getting {
            dependsOn(commonMain)

            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.ktor.client.java)
            }
        }

        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val iosMain by creating {
            dependsOn(commonMain)
            iosX64Main.dependsOn(this)
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)

            dependencies {
                implementation(libs.ktor.client.darwin)
            }
        }
    }
}

android {
    namespace = "org.centrexcursionistalcoi.app"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].res.srcDirs("src/androidMain/res")
    sourceSets["main"].resources.srcDirs("src/commonMain/resources")

    defaultConfig {
        applicationId = "org.centrexcursionistalcoi.app"

        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()

        versionCode = 1
        versionName = "1.0.0"
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    dependencies {
        debugImplementation(libs.compose.ui.tooling)
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"

        buildTypes.release.proguard {
            isEnabled = false
        }

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)

            packageName = "org.centrexcursionistalcoi.app"
            packageVersion = getVersionForPlatform<PlatformVersion>(null).versionName

            description = "CEA App"
            copyright =
                "© ${LocalDate.now().year} Centre Excursionista d'Alcoi & Arnau Mora Gras. All rights reserved."
            vendor = "Centre Excursionista d'Alcoi"

            // val iconsDir = File(rootDir, "icons")

            windows {
                /*iconFile.set(
                    File(iconsDir, "icon.ico")
                )*/
                dirChooser = true
                perUserInstall = true
                menuGroup = "Centre Excursionista d'Alcoi"
                packageName = "CEA App"
                upgradeUuid = "db4a9e58-d033-4ee9-978e-b5065e7f4395"
                val version = getVersionForPlatform(Platform.Windows)
                msiPackageVersion = version.versionName
                exePackageVersion = version.versionName
            }
            linux {
                /*iconFile.set(
                    File(iconsDir, "icon.png")
                )*/
                debMaintainer = "cea.app.linux@arnyminerz.com"
                menuGroup = "Centre Excursionista d'Alcoi"
                appCategory = "Utility"
                val version = getVersionForPlatform(Platform.Linux)
                appRelease = version.versionCode.toString()
                debPackageVersion = version.versionName
                rpmPackageVersion = version.versionName
            }
            macOS {
                /*iconFile.set(
                    File(iconsDir, "icon.icns")
                )*/
                bundleID = "org.centrexcursionistalcoi.app"
                dockName = "Escalar Alcoià i Comtat"
                appStore = true
                appCategory = "public.app-category.utilities"
                val version = getVersionForPlatform(Platform.MacOS)
                dmgPackageVersion = version.versionName
                pkgPackageVersion = version.versionName
                packageBuildVersion = version.versionName
                dmgPackageBuildVersion = version.versionName
                pkgPackageBuildVersion = version.versionName
            }
        }
    }
}

buildkonfig {
    packageName = "buildkonfig"

    defaultConfigs {
        val localProperties = readProperties("local.properties")

        buildConfigField(
            STRING,
            "SUPABASE_URL",
            localProperties["SUPABASE_URL"] as String?
                ?: System.getenv("SUPABASE_URL")
                ?: error("SUPABASE_URL must be set through local.properties or environment variable")
        )
        buildConfigField(
            STRING,
            "SUPABASE_KEY",
            localProperties["SUPABASE_KEY"] as String?
                ?: System.getenv("SUPABASE_KEY")
                ?: error("SUPABASE_KEY must be set through local.properties or environment variable")
        )
    }
}

multiplatformResources {
    multiplatformResourcesPackage = "resources"
}

fun increaseNumberInProperties(key: String) {
    var code = 0
    updateProperties("version.properties") {
        code = getProperty(key).toInt() + 1
        setProperty(key, code.toString())
    }

    println("Increased $key to $code")
}

val increaseVersionCode = task("increaseVersionCode") {
    doFirst {
        increaseNumberInProperties("VERSION_ANDROID_CODE")
    }
}
val increaseLinuxRelease = task("increaseLinuxRelease") {
    doFirst {
        increaseNumberInProperties("VERSION_LIN_RELEASE")
    }
}

tasks.findByName("bundleRelease")?.dependsOn?.add(increaseVersionCode)
tasks.findByName("packageDeb")?.dependsOn?.add(increaseLinuxRelease)
