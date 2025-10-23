package org.centrexcursionistalcoi.app

import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import java.time.Instant
import java.time.LocalDate
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.plugins.configureAuth
import org.centrexcursionistalcoi.app.plugins.configureContentNegotiation
import org.centrexcursionistalcoi.app.plugins.configureRouting
import org.centrexcursionistalcoi.app.plugins.configureSessions
import org.centrexcursionistalcoi.app.plugins.configureStatusPages
import org.centrexcursionistalcoi.app.security.AES
import org.jetbrains.annotations.VisibleForTesting

var today: () -> LocalDate = { LocalDate.now() }
    @VisibleForTesting
    set

var now: () -> Instant = { Instant.now() }
    @VisibleForTesting
    set

fun main() {
    AES.init()

    Database.init(
        url = System.getenv("DB_URL") ?: Database.URL,
        driver = System.getenv("DB_DRIVER"),
        username = System.getenv("DB_USER") ?: "",
        password = System.getenv("DB_PASS") ?: "",
    )
    val isDevelopment = System.getenv("ENV") == "development"

    embeddedServer(
        Netty,
        port = SERVER_PORT,
        host = "0.0.0.0",
        module = { module(isDevelopment = isDevelopment) }
    ).start(wait = true)
}

fun Application.module(isTesting: Boolean = false, isDevelopment: Boolean = false) {
    configureAuth()
    configureContentNegotiation()
    configureRouting()
    configureStatusPages()
    configureSessions(isTesting, isDevelopment)
}
