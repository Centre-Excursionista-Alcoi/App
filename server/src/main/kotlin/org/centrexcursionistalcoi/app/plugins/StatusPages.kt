package org.centrexcursionistalcoi.app.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import org.centrexcursionistalcoi.app.error.Errors
import org.centrexcursionistalcoi.app.error.respondError

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<Exception> { call, exception ->
            call.respondError(Errors.Unknown(exception.message ?: "No message"))
        }
    }
}
