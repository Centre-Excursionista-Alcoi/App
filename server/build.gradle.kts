import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties
import org.gradle.api.tasks.Copy

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

val versionCode = versionProperties.getProperty("VERSION_CODE") ?: error("VERSION_CODE not found in version.properties")

application {
    mainClass.set("org.centrexcursionistalcoi.app.ApplicationKt")
    
    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment", "-Dapp.version=$version", "-Dapp.versionCode=$versionCode")
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21 // Use JVM target 21
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

    // Sentry error tracking, tracing and profiling. Pinned explicitly (see the sentry-jvmSdk
    // comment in gradle/libs.versions.toml) so this can't drift from what io.sentry.jvm.gradle
    // auto-installs.
    implementation(libs.sentry.jvm)
    implementation(libs.sentry.kotlinExtensions)

    // CSV serialization
    implementation(libs.kotlinx.serializationCsv)

    // XML serialization
    implementation(libs.xmlutil.serialization)

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

    // PDF generation
    implementation(libs.pdfbox)

    // Firebase Admin
    implementation(libs.firebase.admin)

    // Telegram bot
    implementation(libs.telegram.bot)
    implementation(libs.retrofit.core)

    // Email sending
    implementation(libs.jakarta.mail)

    testImplementation(kotlin("test-junit5"))
    testImplementation(libs.ktor.server.testHost)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.bundles.testcontainers)
}

// Copies the built admin web panel (Kilua wasmJs bundle, see :admin) into this module's resources, under
// "admin-static/", so it's embedded in the jar and served by AdminRoutes.kt via the classpath (no runtime
// filesystem dependency on :admin's own build directory). `from(...)` a task/task-provider (rather than a
// hardcoded path into :admin/build/...) pulls whatever that task declares as its outputs and wires the task
// dependency automatically, so this doesn't need to track Kotlin/JS's own output-layout conventions.
//
// This does mean `:server:compileTestKotlin`/`test`/`check` now also require a working Node/Yarn toolchain
// (needed to build :admin's wasmJs bundle) -- unavoidable in this repo regardless of where this is wired in,
// since the Sentry plugin already makes `compileTestKotlin` depend on `jar` even without this change.
//
// Guarded on :admin actually being part of the build: the Dockerfile's dependency-cache stage builds a
// deliberately slimmed-down project (no composeApp/android, a stub :shared with no wasmJs target) to warm the
// Gradle cache before the real source is copied in -- :admin isn't included there either, and `project(":admin")`
// would otherwise fail outright at configuration time.
if (findProject(":admin") != null) {
    val copyAdminStatic = tasks.register<Copy>("copyAdminStatic") {
        from(project(":admin").tasks.named("wasmJsBrowserDistribution"))
        into(layout.buildDirectory.dir("generated/adminStatic/admin-static"))
    }

    sourceSets.main {
        resources.srcDir(layout.buildDirectory.dir("generated/adminStatic"))
    }

    tasks.named("processResources") {
        dependsOn(copyAdminStatic)
    }
}

fun Manifest.configureAppManifest() {
    attributes(
        "Implementation-Version" to versionCode,
        "Implementation-Vendor" to "Centre Excursionista d'Alcoi",
        "Specification-Title" to "Centre Excursionista d'Alcoi Server",
        "Specification-Version" to version,
    )
}

tasks.test {
    useJUnitPlatform()

    systemProperty("app.version", version)
    systemProperty("app.versionCode", versionCode)
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
    // The SDK dependency is declared explicitly above (see gradle/libs.versions.toml) so it can be
    // kept in lockstep with sentry-kotlin-extensions -- letting this plugin auto-install its own
    // version risks the same mixed-SDK-versions crash fixed on the Android side.
    autoInstallation {
        enabled = false
    }

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
