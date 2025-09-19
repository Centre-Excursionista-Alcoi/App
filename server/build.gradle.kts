import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlinxSerialization)
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
        freeCompilerArgs.add("-Xcontext-parameters")
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

    // Ktor server
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.server.contentNegotiation)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.sessions)

    // Database
    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.datetime)
    implementation(libs.exposed.json)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.migration.core)
    implementation(libs.exposed.migration.jdbc)
    implementation(libs.h2)
    implementation(libs.sqlite)

    testImplementation(libs.ktor.server.testHost)
    testImplementation(libs.kotlin.testJunit)
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
