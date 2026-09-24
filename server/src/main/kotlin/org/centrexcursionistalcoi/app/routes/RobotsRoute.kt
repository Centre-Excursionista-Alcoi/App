package org.centrexcursionistalcoi.app.routes

import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.robotsRoute() {
    get("/robots.txt") {
        // Disallow all crawlers from downloading files
        call.respondText(
            """
                User-agent: *
                Disallow: /download/
            """.trimIndent()
        )
    }
}
