package org.centrexcursionistalcoi.app.request

import io.ktor.http.ContentType
import io.ktor.http.content.MultiPartData
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.request.contentType
import io.ktor.server.request.receiveMultipart
import io.ktor.server.request.receiveText
import io.ktor.server.routing.RoutingContext
import io.ktor.utils.io.discard
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.asContextElement
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import org.centrexcursionistalcoi.app.error.Error
import org.centrexcursionistalcoi.app.error.respondError
import org.centrexcursionistalcoi.app.json
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("MultipartRequests")

/**
 * Thrown when a file of a request names a part ([org.centrexcursionistalcoi.app.data.FileWithContext.part]) the
 * request doesn't have.
 */
class MissingPartException(part: String) : IllegalArgumentException("The request has no file part named \"$part\"")

/**
 * The files uploaded as parts of the multipart request being handled (see [RequestWithFiles]), by part name.
 * Available while running [ReceivedRequest.withUploads], also inside `Database { }` blocks.
 */
object UploadedParts {
    private val current = ThreadLocal<Map<String, FileRequestData>?>()

    internal fun asContextElement(uploads: Map<String, FileRequestData>): CoroutineContext.Element =
        current.asContextElement(uploads)

    /**
     * Gets the file uploaded in the part named [part].
     * @throws MissingPartException if the request has no such part.
     */
    fun get(part: String): FileRequestData = current.get()?.get(part) ?: throw MissingPartException(part)
}

/**
 * A request received with [receiveRequestWithFiles], and the files uploaded with it, if it was a multipart request.
 */
class ReceivedRequest<T>(val request: T, val uploads: Map<String, FileRequestData> = emptyMap()) {
    /**
     * Runs [block] with [uploads] available through [UploadedParts].
     */
    suspend fun <R> withUploads(block: suspend () -> R): R = withContext(UploadedParts.asContextElement(uploads)) { block() }
}

/**
 * Reads the rest of a multipart request (see [RequestWithFiles]) whose first part, [requestPart], holds the request
 * as JSON. Files are written to temporary files as they are received, never held in memory whole.
 * @throws SerializationException if the request can't be decoded.
 * @throws IllegalArgumentException if two files have the same part name.
 */
suspend fun <T> MultiPartData.readRequestWithFiles(requestPart: PartData.FormItem, serializer: KSerializer<T>): ReceivedRequest<T> {
    val request = try {
        json.decodeFromString(serializer, requestPart.value)
    } catch (e: Exception) {
        discardRemaining()
        throw e
    } finally {
        requestPart.dispose()
    }
    val uploads = mutableMapOf<String, FileRequestData>()
    var duplicate: String? = null
    forEachPart { part ->
        try {
            val name = part.name
            when {
                part !is PartData.FileItem || name == null -> {
                    logger.warn("Ignoring part \"$name\" (${part::class.simpleName}) of multipart request")
                    part.discard()
                }
                // Read the rest of the request anyway, before failing
                name in uploads || duplicate != null -> {
                    duplicate = duplicate ?: name
                    part.discard()
                }
                else -> uploads[name] = FileRequestData().apply { populate(part) }
            }
        } finally {
            part.dispose()
        }
    }
    duplicate?.let { throw IllegalArgumentException("There are two file parts named \"$it\"") }
    return ReceivedRequest(request, uploads)
}

private suspend fun PartData.discard() {
    if (this is PartData.FileItem) provider().discard()
}

/**
 * Reads the rest of the request, discarding it. The whole request must be read before responding, also with an
 * error.
 */
suspend fun MultiPartData.discardRemaining() {
    forEachPart { part ->
        try {
            part.discard()
        } finally {
            part.dispose()
        }
    }
}

/**
 * Checks that the request is JSON or multipart, the ways a [RequestWithFiles] can be sent. Responds with an error
 * and returns `null` otherwise.
 */
suspend fun RoutingContext.assertRequestWithFilesContentType(): Unit? {
    val contentType = call.request.contentType()
    if (!contentType.match(ContentType.Application.Json) && !contentType.match(ContentType.MultiPart.FormData)) {
        respondError(Error.InvalidContentType(ContentType.Application.Json, contentType))
        return null
    }
    return Unit
}

/**
 * Receives a request that can carry files ([RequestWithFiles]): as JSON, or as multipart with the files in parts
 * of their own.
 * @param decodingError The error to respond with if the request can't be decoded.
 * @return The request, or `null` if it couldn't be received. An error has been responded then.
 */
suspend fun <T> RoutingContext.receiveRequestWithFiles(
    serializer: KSerializer<T>,
    decodingError: (cause: Exception, body: String?) -> Error = { _, _ -> Error.MalformedRequest() },
): ReceivedRequest<T>? {
    val contentType = call.request.contentType()
    when {
        contentType.match(ContentType.Application.Json) -> {
            val body = call.receiveText()
            return try {
                ReceivedRequest(json.decodeFromString(serializer, body))
            } catch (e: SerializationException) {
                logger.error("Failed to decode request. Body: $body", e)
                respondError(decodingError(e, body))
                null
            } catch (e: IllegalArgumentException) {
                logger.error("Failed to decode request. Body: $body", e)
                respondError(decodingError(e, body))
                null
            }
        }
        contentType.match(ContentType.MultiPart.FormData) -> {
            val multipart = call.receiveMultipart()
            val first = multipart.readPart()
            if (first !is PartData.FormItem || first.name != RequestWithFiles.REQUEST_PART) {
                first?.discard()
                first?.dispose()
                multipart.discardRemaining()
                logger.error("Multipart request doesn't start with the \"${RequestWithFiles.REQUEST_PART}\" part")
                respondError(Error.MalformedRequest())
                return null
            }
            return try {
                multipart.readRequestWithFiles(first, serializer)
            } catch (e: SerializationException) {
                logger.error("Failed to decode multipart request", e)
                respondError(decodingError(e, null))
                null
            } catch (e: IllegalArgumentException) {
                logger.error("Failed to decode multipart request", e)
                respondError(decodingError(e, null))
                null
            }
        }
        else -> {
            respondError(Error.InvalidContentType(ContentType.Application.Json, contentType))
            return null
        }
    }
}

/**
 * Gives [first] back as the first part of [rest], once it has been read to find out what kind of request it is.
 */
internal class PushedBackMultiPartData(private var first: PartData?, private val rest: MultiPartData) : MultiPartData {
    override suspend fun readPart(): PartData? = first?.also { first = null } ?: rest.readPart()
}
