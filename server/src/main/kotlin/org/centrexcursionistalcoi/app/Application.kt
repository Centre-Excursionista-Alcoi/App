package org.centrexcursionistalcoi.app

import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.sentry.Sentry
import java.time.Instant
import java.time.LocalDate
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.notifications.Push
import org.centrexcursionistalcoi.app.plugins.configureContentNegotiation
import org.centrexcursionistalcoi.app.plugins.configureRouting
import org.centrexcursionistalcoi.app.plugins.configureSessions
import org.centrexcursionistalcoi.app.plugins.configureStatusPages
import org.centrexcursionistalcoi.app.security.AES
import org.jetbrains.annotations.VisibleForTesting
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("Application")

var today: () -> LocalDate = { LocalDate.now() }
    @VisibleForTesting
    set

var now: () -> Instant = { Instant.now() }
    @VisibleForTesting
    set

fun main() {
    logger.info("Starting Centre Excursionista d'Alcoi server version $version")

    System.getenv("SENTRY_DSN")?.let { dsn ->
        Sentry.init { options ->
            options.dsn = dsn
            options.release = version
            options.environment = System.getenv("ENV") ?: "production"
        }
    } ?: logger.warn("SENTRY_DSN environment variable is not set. Sentry error tracking is disabled.")

    AES.init()

    Database.init(
        url = System.getenv("DB_URL") ?: Database.URL,
        driver = System.getenv("DB_DRIVER"),
        username = System.getenv("DB_USER") ?: "",
        password = System.getenv("DB_PASS") ?: "",
    )
    val isDevelopment = System.getenv("ENV") == "development"

    Push.init()

    embeddedServer(
        Netty,
        port = SERVER_PORT,
        host = "0.0.0.0",
        module = { module(isDevelopment = isDevelopment) }
    ).start(wait = true)
}

fun Application.module(isTesting: Boolean = false, isDevelopment: Boolean = false) {
    configureContentNegotiation()
    configureRouting()
    configureStatusPages()
    configureSessions(isTesting, isDevelopment)
}
