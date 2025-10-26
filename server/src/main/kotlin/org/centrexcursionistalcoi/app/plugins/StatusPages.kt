package org.centrexcursionistalcoi.app.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.sentry.Sentry
import org.centrexcursionistalcoi.app.error.Error
import org.centrexcursionistalcoi.app.error.respondError

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<Exception> { call, exception ->
            Sentry.captureException(exception)
            call.respondError(Error.Unknown(exception.message ?: "No message"))
        }
    }
}
