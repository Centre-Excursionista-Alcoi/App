package org.centrexcursionistalcoi.app.routes

import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.post
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Defines a POST route with a [Mutex] to ensure that only one request can be processed at a time.
 * This is useful for endpoints that modify shared resources and need to be thread-safe.
 * @param path The path for the POST route.
 * @param mutex The [Mutex] to use for locking.
 * @param body The body of the route, which will be executed within the lock.
 * @return The created [Route].
 */
fun Route.postWithLock(
    path: String,
    mutex: Mutex,
    body: suspend RoutingContext.() -> Unit
): Route = post(path) {
    mutex.withLock {
        body()
    }
}
