import io.ktor.plugin.features.DockerImageRegistry
import io.ktor.plugin.features.DockerPortMapping
import io.ktor.plugin.features.DockerPortMappingProtocol
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.sentry.jvm)
    alias(libs.plugins.serialization)
    application
}

group = "org.centrexcursionistalcoi.app"
version = System.getenv("VERSION") ?: "development"

application {
    mainClass.set("org.centrexcursionistalcoi.app.ApplicationKt")
    // applicationDefaultJvmArgs = listOf("-Dio.ktor.development=${extra["io.ktor.development"] ?: "false"}")
}

dependencies {
    implementation(projects.shared)
    implementation(libs.logback)

    implementation(libs.kotlinx.serialization.json)

    implementation(libs.ktor.serialization.kotlinxJson)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.contentNegotiation)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.sessions)
    implementation(libs.ktor.server.statusPages)

    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.javaTime)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.json)
    implementation(libs.exposed.money)
    implementation(libs.h2)
    implementation(libs.postgresql)

    implementation(libs.javax.money.api)
    implementation(libs.javax.money.impl)

    implementation(libs.firebase.admin)

    implementation(libs.kreds)

    implementation(libs.sentry.javaIo)

    testImplementation(libs.ktor.server.tests)
    testImplementation(libs.kotlin.test.junit)
}

ktor {
    docker {
        jreVersion.set(JavaVersion.VERSION_21)

        localImageName.set("cea-app")
        imageTag.set(version.toString())

        portMappings.set(listOf(
            DockerPortMapping(80, 8080, DockerPortMappingProtocol.TCP)
        ))

        externalRegistry.set(
            DockerImageRegistry.dockerHub(
                appName = provider { "cea-app" },
                username = providers.environmentVariable("DOCKER_HUB_USERNAME"),
                password = providers.environmentVariable("DOCKER_HUB_PASSWORD")
            )
        )
    }
}

fun readPropertiesFile(path: String): Properties? {
    val file = rootProject.file(path)
    if (!file.exists()) {
        return null
    }
    val props = Properties()
    file.inputStream().use { props.load(it) }
    return props
}

sentry {
    // Generates a JVM (Java, Kotlin, etc.) source bundle and uploads your source code to Sentry.
    // This enables source context, allowing you to see your source
    // code as part of your stack traces in Sentry.
    includeSourceContext = true

    org = "centre-excursionista-alcoi"
    projectName = "server"

    val secrets = readPropertiesFile("secrets.properties")
    val token = secrets?.get("SENTRY_AUTH_TOKEN_SERVER") as String? ?: error("SENTRY_AUTH_TOKEN_SERVER not found in secrets.properties")
    authToken = token
}
