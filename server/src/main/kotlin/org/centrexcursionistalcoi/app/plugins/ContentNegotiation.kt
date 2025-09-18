package org.centrexcursionistalcoi.app.plugins

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import org.centrexcursionistalcoi.app.json

fun Application.configureContentNegotiation() {
    // Install ContentNegotiation feature
    install(ContentNegotiation) {
        json(json)
    }
}
