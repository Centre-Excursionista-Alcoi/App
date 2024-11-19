package org.centrexcursionistalcoi.app.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.path
import io.ktor.server.response.respondText
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("StatusPages")

fun Application.installStatusPages() {
    install(StatusPages) {
        exception { call: ApplicationCall, cause: Exception ->
            logger.error("Got an unhandled exception at ${call.request.path()}", cause)
            call.respondText(text = "500: Internal Server Error.\n$cause", status = HttpStatusCode.InternalServerError)
        }
        status(HttpStatusCode.NotFound) { call, status ->
            call.respondText(text = "404: Page Not Found", status = status)
        }
    }
}
