package org.centrexcursionistalcoi.app.plugins

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import org.centrexcursionistalcoi.app.serverJson

fun Application.installContentNegotiation() {
    install(ContentNegotiation) {
        json(serverJson)
    }
}
