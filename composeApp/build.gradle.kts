import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
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

                // Compose - Voyager
                implementation(libs.voyager.navigator)
                implementation(libs.voyager.screenModel)

                // Moko Resources
                implementation(libs.moko.resources.base)
                implementation(libs.moko.resources.compose)

                // Supabase
                implementation(libs.supabase.auth)
                implementation(libs.supabase.realtime)

                // Multiplatform Settings
                implementation(libs.multiplatformSettings.base)
                implementation(libs.multiplatformSettings.coroutines)
                implementation(libs.multiplatformSettings.serialization)
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

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "org.centrexcursionistalcoi.app"
            packageVersion = "1.0.0"
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
