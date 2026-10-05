package org.centrexcursionistalcoi.app.routes

import io.ktor.http.HttpMethod
import io.ktor.http.URLBuilder
import io.ktor.http.fullPath
import io.ktor.resources.href
import io.ktor.server.application.plugin
import io.ktor.server.resources.Resources
import io.ktor.server.resources.get
import io.ktor.server.resources.handle as handleResource
import io.ktor.server.resources.patch
import io.ktor.server.resources.post
import io.ktor.server.resources.resource
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.method
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.KSerializer

/**
 * Defines a POST route for the resource [T] with a [Mutex] to ensure that only one request can be processed at a time.
 * This is useful for endpoints that modify shared resources and need to be thread-safe.
 * @param mutex The [Mutex] to use for locking.
 * @param body The body of the route, which will be executed within the lock.
 * @return The created [Route].
 */
inline fun <reified T : Any> Route.postWithLock(
    mutex: Mutex,
    noinline body: suspend RoutingContext.(T) -> Unit
): Route = post<T> { resource ->
    mutex.withLock {
        body(resource)
    }
}

/**
 * Like [postWithLock], for PATCH.
 */
inline fun <reified T : Any> Route.patchWithLock(
    mutex: Mutex,
    noinline body: suspend RoutingContext.(T) -> Unit
): Route = patch<T> { resource ->
    mutex.withLock {
        body(resource)
    }
}

/**
 * Defines a GET route for the resource [T] with a [Mutex] to ensure that only one request can be processed at a time.
 * This is useful for endpoints that read shared resources and need to be thread-safe.
 * @param mutex The [Mutex] to use for locking.
 * @param body The body of the route, which will be executed within the lock.
 * @return The created [Route].
 */
inline fun <reified T : Any> Route.getWithLock(
    mutex: Mutex,
    noinline body: suspend RoutingContext.(T) -> Unit
): Route = get<T> { resource ->
    mutex.withLock {
        body(resource)
    }
}

/**
 * Registers [body] for [method] on the resource described by [serializer]. Like Ktor's `get<T>`, `post<T>`, etc., for
 * resources only known at runtime (see [provideEntityRoutes]).
 */
fun <T : Any> Route.handle(
    serializer: KSerializer<T>,
    method: HttpMethod,
    body: suspend RoutingContext.(T) -> Unit,
): Route {
    lateinit var builtRoute: Route
    resource(serializer) {
        builtRoute = method(method) { handleResource(serializer, body) }
    }
    return builtRoute
}

/** The path (and query) of [resource]. Like Ktor's `href`, for resources only known at runtime. */
fun <T> RoutingContext.href(serializer: KSerializer<T>, resource: T): String {
    val urlBuilder = URLBuilder()
    href(call.application.plugin(Resources).resourcesFormat, serializer, resource, urlBuilder)
    return urlBuilder.build().fullPath
}
