package org.centrexcursionistalcoi.app.error

import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingContext

suspend fun ApplicationCall.respondError(error: Error) {
    response.header("CEA-Error-Code", error.code.toString())
    respond(error.statusCode, error)
}

suspend fun RoutingContext.respondError(error: Error) {
    call.respondError(error)
}
