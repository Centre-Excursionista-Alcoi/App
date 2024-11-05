package org.centrexcursionistalcoi.app.endpoints

import io.ktor.http.HttpMethod
import io.ktor.server.routing.RoutingContext

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
}
