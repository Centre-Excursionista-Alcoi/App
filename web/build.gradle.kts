import com.varabyte.kobweb.gradle.application.util.configAsKobwebApplication
import java.util.Properties

plugins {
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinxSerialization)
    // must be applied after kotlin-multiplatform
    alias(libs.plugins.kobwebApplication)
    alias(libs.plugins.kobwebxMarkdown)
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

val versionProperties = readProperties("version.properties", rootProject.rootDir) ?: error("Could not read version.properties")

group = "org.centrexcursionistalcoi.web"
version = versionProperties.getProperty("VERSION_NAME") ?: error("VERSION_NAME not found in version.properties")

kobweb {
    app {
        index {
            description.set("Powered by Kobweb")
        }
    }
}

kotlin {
    configAsKobwebApplication("web")

    sourceSets {
        jsMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.html.core)

            // The whole Kobweb framework
            implementation(libs.bundles.kobweb)

            implementation(libs.kobwebx.markdown)

            // Ktor client
            implementation(libs.ktor.client.auth)
            implementation(libs.ktor.client.contentNegotiation)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.logging)

            // Ktor engine for JS
            implementation(libs.ktor.client.js)

            implementation(projects.shared)
        }
    }
}
