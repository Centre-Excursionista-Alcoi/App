package org.centrexcursionistalcoi.app.error

import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingContext

suspend fun RoutingContext.respondError(error: Error) {
    call.response.header("CEA-Error-Code", error.code.toString())
    call.respond(error.statusCode, error)
}
