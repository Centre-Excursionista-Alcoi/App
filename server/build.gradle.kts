plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlinxSerialization)
    application
}

group = "org.centrexcursionistalcoi.app"
version = "1.0.0"

application {
    mainClass.set("org.centrexcursionistalcoi.app.ApplicationKt")
    
    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

dependencies {
    implementation(projects.shared)
    implementation(libs.logback)

    // Ktor client
    implementation(libs.ktor.client.auth)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.java)

    // Ktor server
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.server.contentNegotiation)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.serialization.kotlinxJson)
    implementation(libs.ktor.server.sessions)

    // Database
    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.datetime)
    implementation(libs.exposed.r2dbc)
    implementation(libs.exposed.migration.core)
    implementation(libs.exposed.migration.r2dbc)
    implementation(libs.h2)

    testImplementation(libs.ktor.server.testHost)
    testImplementation(libs.kotlin.testJunit)
}

tasks.register<JavaExec>("generateMigrationScript") {
    group = "application"
    description = "Generate a migration script in the path src/main/kotlin/org/centrexcursionistalcoi/app/database/migrations"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "org.centrexcursionistalcoi.app.database.GenerateMigrationScriptKt"
}
