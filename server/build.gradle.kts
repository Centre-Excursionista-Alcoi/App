import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.kover)
    application
}

group = "org.centrexcursionistalcoi.app"
version = "2.0.0"

application {
    mainClass.set("org.centrexcursionistalcoi.app.ApplicationKt")
    
    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
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
}

tasks.test {
    useJUnitPlatform()
}

tasks.register<JavaExec>("generateMigrationScript") {
    group = "application"
    description = "Generate a migration script in the path src/main/kotlin/org/centrexcursionistalcoi/app/database/migrations"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "org.centrexcursionistalcoi.app.database.GenerateMigrationScriptKt"
}

tasks.withType<ShadowJar> {
    // Make sure all drivers are included in the fat jar
    mergeServiceFiles()
}
