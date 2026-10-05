package org.centrexcursionistalcoi.app.request

import io.ktor.http.ContentType
import io.ktor.server.request.receiveText
import io.ktor.server.routing.RoutingContext
import kotlinx.serialization.KSerializer
import org.centrexcursionistalcoi.app.error.Error
import org.centrexcursionistalcoi.app.error.respondError
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.routes.assertContentType
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("JsonRequests")

/**
 * Receives a JSON request.
 * @return The request, or `null` if it couldn't be received. An error has been responded then.
 */
suspend fun <T> RoutingContext.receiveJson(serializer: KSerializer<T>): T? {
    assertContentType(ContentType.Application.Json) ?: return null
    return try {
        json.decodeFromString(serializer, call.receiveText())
    } catch (e: IllegalArgumentException) {
        // SerializationException is an IllegalArgumentException. The body isn't logged, it may hold passwords.
        logger.error("Failed to decode ${serializer.descriptor.serialName}", e)
        respondError(Error.MalformedRequest())
        null
    }
}

/**
 * Receives a JSON request that may be left out: an empty body gives [serializer]'s request with all its defaults.
 * @return The request, or `null` if it couldn't be received. An error has been responded then.
 */
suspend fun <T> RoutingContext.receiveOptionalJson(serializer: KSerializer<T>): T? {
    val body = call.receiveText()
    if (body.isBlank()) return json.decodeFromString(serializer, "{}")
    return try {
        json.decodeFromString(serializer, body)
    } catch (e: IllegalArgumentException) {
        logger.error("Failed to decode ${serializer.descriptor.serialName}", e)
        respondError(Error.MalformedRequest())
        null
    }
}
