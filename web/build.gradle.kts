import com.varabyte.kobweb.gradle.application.util.configAsKobwebApplication

plugins {
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinxSerialization)
    // must be applied after kotlin-multiplatform
    alias(libs.plugins.kobwebApplication)
    alias(libs.plugins.kobwebxMarkdown)
}

group = "org.centrexcursionistalcoi.web"
version = "1.0-SNAPSHOT"

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
