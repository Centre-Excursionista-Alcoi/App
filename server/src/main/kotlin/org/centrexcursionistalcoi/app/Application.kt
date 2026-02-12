package org.centrexcursionistalcoi.app

import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.sentry.Sentry
import java.time.Instant
import java.time.LocalDate
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.DatabaseNowExpression
import org.centrexcursionistalcoi.app.integration.CEA
import org.centrexcursionistalcoi.app.notifications.Push
import org.centrexcursionistalcoi.app.plugins.SessionsKeys
import org.centrexcursionistalcoi.app.plugins.configureContentNegotiation
import org.centrexcursionistalcoi.app.plugins.configureRouting
import org.centrexcursionistalcoi.app.plugins.configureSSE
import org.centrexcursionistalcoi.app.plugins.configureSessions
import org.centrexcursionistalcoi.app.plugins.configureStatusPages
import org.centrexcursionistalcoi.app.security.AES
import org.jetbrains.annotations.TestOnly
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("Application")

var today: () -> LocalDate = { LocalDate.now() }
    private set

var now: () -> Instant = { Instant.now() }
    private set

@TestOnly
internal fun mockTime(instant: Instant) {
    now = { instant }
    DatabaseNowExpression.mockTime(instant)
}

@TestOnly
internal fun mockTime(date: LocalDate) {
    today = { date }
}

@TestOnly
internal fun resetTimeFunctions() {
    today = { LocalDate.now() }
    now = { Instant.now() }
    DatabaseNowExpression.reset()
}

fun main() {
    logger.info("Starting Centre Excursionista d'Alcoi server version $version")

    // Initialize Sentry error tracking if DSN is provided
    System.getenv("SENTRY_DSN")?.let { dsn ->
        Sentry.init { options ->
            options.dsn = dsn
            options.release = version
            options.environment = System.getenv("ENV") ?: "production"
        }
    } ?: logger.warn("SENTRY_DSN environment variable is not set. Sentry error tracking is disabled.")

    // Initialize AES encryption
    AES.init()

    // Validate Session encryption keys
    if (SessionsKeys.secretEncryptKey == null || SessionsKeys.secretSignKey == null) {
        logger.warn("No Session encryption keys found. Using default keys. This is a security issue.")
        logger.warn("Suggestion: Set the SECRET_ENCRYPT_KEY and SECRET_SIGN_KEY environment variables.")
    } else {
        logger.info("Session encryption keys found.")
    }

    // Initialize Database connection
    val dbInitResult = Database.init(
        url = System.getenv("DB_URL") ?: Database.URL,
        driver = System.getenv("DB_DRIVER"),
        username = System.getenv("DB_USER") ?: "",
        password = System.getenv("DB_PASS") ?: "",
    )
    val isDevelopment = System.getenv("ENV") == "development"

    // Initialize Push Notification service - Firebase Cloud Messaging
    Push.initFCM()

    // Start periodic CEA synchronization
    CEA.start(
        waitUntilFirstSync = dbInitResult and Database.INIT_RESULT_MIGRATION_EXECUTED == Database.INIT_RESULT_MIGRATION_EXECUTED
    )

    // Start Ktor server
    embeddedServer(
        Netty,
        port = SERVER_PORT,
        host = "0.0.0.0",
        module = { module(isDevelopment = isDevelopment) }
    ).start(wait = true)
}

fun Application.module(isTesting: Boolean = false, isDevelopment: Boolean = false) {
    configureContentNegotiation()
    configureSSE()
    configureRouting()
    configureStatusPages()
    configureSessions(isTesting, isDevelopment)
}
