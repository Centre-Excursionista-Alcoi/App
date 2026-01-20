import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.googleServices)
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

val credentialsProperties = readProperties("credentials.properties", rootDir)
fun getCredential(key: String): String? {
    val gradleProperty = providers.gradleProperty(key)
    return System.getenv(key) ?: gradleProperty.orNull ?: credentialsProperties?.getProperty(key)
}

android {
    namespace = "org.centrexcursionistalcoi.app"
    compileSdk {
        version = release(libs.versions.android.compileSdk.get().toInt())
    }

    defaultConfig {
        applicationId = "org.centrexcursionistalcoi.app"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionName = appVersionName
        versionCode = appVersionCode.toInt()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        release {
            isMinifyEnabled = true
            isShrinkResources = true

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(projects.composeApp)
    coreLibraryDesugaring(libs.android.desugaring)
}
