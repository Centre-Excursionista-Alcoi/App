package org.centrexcursionistalcoi.app.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.routing.Route
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import io.ktor.server.sse.SSE
import io.ktor.server.sse.sse
import java.util.UUID
import org.centrexcursionistalcoi.app.notifications.Push
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("SSE")

fun Application.configureSSE() {
    install(SSE)
}

fun Route.configureSSERoutes() {
    sse("/events") {
        val session = call.sessions.get<UserSession>()
        if (session == null) {
            logger.debug("User tried to connect to SSE without a valid session.")
            close()
            return@sse
        }

        logger.info("SSE connection established for user: ${session.sub} (isAdmin=${session.isAdmin()})")
        send(event = "connection")

        Push.flow(session).collect { notification ->
            val data = notification.toMap()
                .toList()
                .joinToString("&") { (key, value) -> "$key=$value" }

            logger.debug("Sending SSE event '${notification.type}' to user ${session.sub} with data: $data")
            send(
                data = data,
                event = notification.type,
                id = UUID.randomUUID().toString(),
            )
        }
    }
}
