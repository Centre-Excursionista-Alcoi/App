package org.centrexcursionistalcoi.app.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages

fun Application.configureStatusPages() {
    install(StatusPages) {
        // TODO: Configure status pages
    }
}
