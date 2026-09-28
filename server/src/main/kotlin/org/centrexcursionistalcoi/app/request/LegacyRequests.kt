package org.centrexcursionistalcoi.app.request

import io.ktor.http.ContentType
import io.ktor.http.Parameters
import io.ktor.http.content.MultiPartData
import io.ktor.http.content.PartData
import io.ktor.server.request.contentType
import io.ktor.server.request.receiveMultipart
import io.ktor.server.request.receiveParameters
import io.ktor.server.request.receiveText
import io.ktor.server.routing.RoutingContext
import kotlinx.serialization.KSerializer
import org.centrexcursionistalcoi.app.error.Error
import org.centrexcursionistalcoi.app.error.respondError
import org.centrexcursionistalcoi.app.json
import org.slf4j.LoggerFactory

// Receiving requests both as JSON and as the forms app versions that predate JSON send. Once the oldest app version
// still served sends JSON, replace these with plain JSON (or receiveRequestWithFiles) and remove the form parsing.

private val logger = LoggerFactory.getLogger("LegacyRequests")

/**
 * Receives a request sent as JSON or, by app versions that predate JSON, as a form, which [fromForm] converts.
 * @param fromForm Converts the form into the request, or responds with an error and returns `null`.
 * @return The request, or `null` if it couldn't be received. An error has been responded then.
 */
suspend fun <T> RoutingContext.receiveJsonOrForm(
    serializer: KSerializer<T>,
    fromForm: suspend RoutingContext.(Parameters) -> T?,
): T? {
    val contentType = call.request.contentType()
    return when {
        contentType.match(ContentType.Application.Json) -> {
            try {
                json.decodeFromString(serializer, call.receiveText())
            } catch (e: IllegalArgumentException) {
                // SerializationException is an IllegalArgumentException. The body isn't logged, it may hold passwords.
                logger.error("Failed to decode ${serializer.descriptor.serialName}", e)
                respondError(Error.MalformedRequest())
                null
            }
        }
        contentType.match(ContentType.Application.FormUrlEncoded) -> fromForm(call.receiveParameters())
        else -> {
            respondError(Error.InvalidContentType(ContentType.Application.Json, contentType))
            null
        }
    }
}

/**
 * Receives a request that can carry files (see [receiveRequestWithFiles]), or the multipart form app versions that
 * predate it send, which [fromMultipart] converts.
 * @param fromMultipart Reads the whole form, and converts it into the request, with its files as uploaded parts. Or
 * responds with an error and returns `null`.
 * @return The request, or `null` if it couldn't be received. An error has been responded then.
 */
suspend fun <T> RoutingContext.receiveRequestWithFilesOrMultipart(
    serializer: KSerializer<T>,
    fromMultipart: suspend RoutingContext.(MultiPartData) -> ReceivedRequest<T>?,
): ReceivedRequest<T>? {
    if (!call.request.contentType().match(ContentType.MultiPart.FormData)) {
        return receiveRequestWithFiles(serializer)
    }
    val multipart = call.receiveMultipart()
    val first = multipart.readPart()
    if (first is PartData.FormItem && first.name == RequestWithFiles.REQUEST_PART) {
        return try {
            multipart.readRequestWithFiles(first, serializer)
        } catch (e: Exception) {
            logger.error("Failed to decode multipart ${serializer.descriptor.serialName}", e)
            respondError(Error.MalformedRequest())
            null
        }
    }
    return fromMultipart(PushedBackMultiPartData(first, multipart))
}
