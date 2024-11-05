package org.centrexcursionistalcoi.app.endpoints.model

import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.header
import io.ktor.server.response.respondText
import io.ktor.server.routing.RoutingContext
import org.centrexcursionistalcoi.app.server.response.ErrorResponse

abstract class Endpoint(
    val route: String,
    val method: HttpMethod = HttpMethod.Get
) {
    suspend operator fun invoke(context: RoutingContext) {
        with(context) {
            body()
        }
    }

    abstract suspend fun RoutingContext.body()

    protected suspend fun RoutingContext.respondSuccess(
        status: HttpStatusCode = HttpStatusCode.OK
    ) {
        call.respondText("OK", contentType = ContentType.Text.Plain, status = status)
    }

    protected suspend fun RoutingContext.respondFailure(
        error: ErrorResponse
    ) {
        val status = HttpStatusCode.fromValue(error.httpStatusCode)

        call.response.header("X-Error-Code", error.code)
        call.respondText(error.message, contentType = ContentType.Text.Plain, status = status)
    }
}
