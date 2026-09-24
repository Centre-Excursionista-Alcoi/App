package org.centrexcursionistalcoi.app.routes

import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.robotsRoute() {
    get("/robots.txt") {
        // Disallow all crawlers from indexing this server, because it contains sensitive data (user accounts, events, etc.) and is not meant to be public.
        call.respondText(
            """
                User-agent: *
                Disallow: /
            """.trimIndent()
        )
    }
}
