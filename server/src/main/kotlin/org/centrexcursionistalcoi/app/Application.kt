package org.centrexcursionistalcoi.app

import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.centrexcursionistalcoi.app.database.ServerDatabase
import org.centrexcursionistalcoi.app.plugins.configureRouting
import org.centrexcursionistalcoi.app.plugins.installAuthentication
import org.centrexcursionistalcoi.app.plugins.installContentNegotiation
import org.centrexcursionistalcoi.app.plugins.installSessions
import org.centrexcursionistalcoi.app.plugins.installStatusPages

fun main() = runBlocking {
    start()
}

private suspend fun start() {
    withTimeout(10_000) {
        val databaseUrl = System.getenv("DATABASE_URL") ?: "jdbc:h2:file:./CEA"
        val databaseDriver = System.getenv("DATABASE_DRIVER") ?: "org.h2.Driver"
        val databaseUsername = System.getenv("DATABASE_USERNAME") ?: ""
        val databasePassword = System.getenv("DATABASE_PASSWORD") ?: ""

        ServerDatabase.initialize(databaseUrl, databaseDriver, databaseUsername, databasePassword)
    }

    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    installAuthentication()
    installContentNegotiation()
    configureRouting()
    installSessions()
    installStatusPages()
}
