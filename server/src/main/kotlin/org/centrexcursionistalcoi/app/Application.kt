package org.centrexcursionistalcoi.app

import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.runBlocking
import org.centrexcursionistalcoi.app.database.ServerDatabase
import org.centrexcursionistalcoi.app.plugins.configureRouting
import org.centrexcursionistalcoi.app.plugins.installAuthentication
import org.centrexcursionistalcoi.app.plugins.installContentNegotiation
import org.centrexcursionistalcoi.app.plugins.installSessions

fun main() = runBlocking<Unit> {
    start()
}

private suspend fun start() {
    ServerDatabase.initialize()

    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    installAuthentication()
    installContentNegotiation()
    configureRouting()
    installSessions()
}
