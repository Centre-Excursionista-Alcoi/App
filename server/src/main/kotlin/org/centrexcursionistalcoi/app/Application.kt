package org.centrexcursionistalcoi.app

import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.runBlocking
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.plugins.configureAuth
import org.centrexcursionistalcoi.app.plugins.configureContentNegotiation
import org.centrexcursionistalcoi.app.plugins.configureRouting
import org.centrexcursionistalcoi.app.plugins.configureSessions

fun main() {
    runBlocking { Database.init() }

    embeddedServer(Netty, port = SERVER_PORT, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module(isTesting: Boolean = false) {
    configureAuth()
    configureContentNegotiation()
    configureRouting()
    configureSessions(isTesting)
}
