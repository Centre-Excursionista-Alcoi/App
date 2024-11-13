plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.serialization)
    application
}

group = "org.centrexcursionistalcoi.app"
version = "1.0.0"

application {
    mainClass.set("org.centrexcursionistalcoi.app.ApplicationKt")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=${extra["io.ktor.development"] ?: "false"}")
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
    implementation(libs.h2)

    testImplementation(libs.ktor.server.tests)
    testImplementation(libs.kotlin.test.junit)
}

ktor {
    docker {
        jreVersion.set(JavaVersion.VERSION_21)
    }
}
