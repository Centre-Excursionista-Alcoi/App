package org.centrexcursionistalcoi.app.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.basic
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("Authentication")

const val BASIC_AUTH_NAME = "auth-basic"

fun Application.installAuthentication() {
    logger.debug("Installing authentication plugin...")
    install(Authentication) {
        basic(BASIC_AUTH_NAME) {
            realm = "Access to endpoints with basic auth"
            validate { credentials ->
                logger.debug("Got request with credentials: ${credentials.name} :: ${credentials.password}")
                if (credentials.name.isNotBlank() && credentials.password.isNotBlank()) {
                    credentials
                } else {
                    null
                }
            }
        }
    }
}
