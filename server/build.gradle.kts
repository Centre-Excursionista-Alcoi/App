import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.kover)
    alias(libs.plugins.sentryJvm)
    application
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

val credentialsProperties = readProperties("credentials.properties", rootDir)
fun getCredential(key: String): String? {
    val gradleProperty = providers.gradleProperty(key)
    return System.getenv(key) ?: gradleProperty.orNull ?: credentialsProperties?.getProperty(key)
}

val versionProperties = readProperties("version.properties", rootProject.rootDir) ?: error("Could not read version.properties")

group = "org.centrexcursionistalcoi.app"
version = versionProperties.getProperty("VERSION_NAME") ?: error("VERSION_NAME not found in version.properties")

application {
    mainClass.set("org.centrexcursionistalcoi.app.ApplicationKt")
    
    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment", "-Dapp.version=$version")
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21 // Use JVM target 21
        freeCompilerArgs.add("-Xcontext-parameters")
        optIn.add("kotlin.uuid.ExperimentalUuidApi")
        optIn.add("kotlin.time.ExperimentalTime")
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21)) // Use Java 21
    }
}

dependencies {
    implementation(projects.shared)
    implementation(libs.logback)

    // CSV serialization
    implementation(libs.kotlinx.serializationCsv)

    // Ktor serialization
    implementation(libs.ktor.serialization.kotlinxJson)

    // Ktor client
    implementation(libs.ktor.client.auth)
    implementation(libs.ktor.client.contentNegotiation)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.java)
    implementation(libs.ktor.client.logging)

    // Ktor server
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.server.contentNegotiation)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.sessions)
    implementation(libs.ktor.server.sse)
    implementation(libs.ktor.server.statusPages)

    // Database
    implementation(libs.exposed.core)
    implementation(libs.exposed.crypt)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.datetime)
    implementation(libs.exposed.json)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.migration.core)
    implementation(libs.exposed.migration.jdbc)
    implementation(libs.h2)
    implementation(libs.postgresql)
    implementation(libs.sqlite)

    // Encryption
    implementation(libs.bcrypt)

    // Redis
    implementation(libs.kreds)

    // XML Parsing
    implementation(libs.ksoup.core)
    implementation(libs.ksoup.network)

    // Firebase Admin
    implementation(libs.firebase.admin)

    testImplementation(kotlin("test-junit5"))
    testImplementation(libs.ktor.server.testHost)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.mockk)
    testImplementation(libs.bundles.testcontainers)
}

fun Manifest.configureAppManifest() {
    attributes(
        "Implementation-Version" to version
    )
}

tasks.test {
    useJUnitPlatform()

    systemProperty("app.version", version)
}

// Regular (thin) JAR
tasks.jar {
    manifest.configureAppManifest()
}

// Fat (shadow) JAR
tasks.withType<ShadowJar> {
    // Make sure all drivers are included in the fat jar
    mergeServiceFiles()

    // Add manifest attributes
    manifest.configureAppManifest()
}

sentry {
    // Generates a JVM (Java, Kotlin, etc.) source bundle and uploads your source code to Sentry.
    // This enables source context, allowing you to see your source
    // code as part of your stack traces in Sentry.
    includeSourceContext = true

    org = "centre-excursionista-alcoi"
    projectName = "server"
    authToken = getCredential("SENTRY_AUTH_TOKEN").also {
        if (it == null) System.err.println("SENTRY_AUTH_TOKEN was not given, source code won't be uploaded to Sentry")
    }
}
