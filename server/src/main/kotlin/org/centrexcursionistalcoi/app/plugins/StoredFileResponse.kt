package org.centrexcursionistalcoi.app.plugins

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.toHttpDate
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.header
import io.ktor.server.response.respondOutputStream
import io.ktor.server.response.respondText
import io.ktor.util.date.GMTDate
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.centrexcursionistalcoi.app.storage.FileStorageProvider
import org.centrexcursionistalcoi.app.storage.StoredObjectNotFoundException
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("StoredFileResponse")

/**
 * Responds with the contents of a stored file, streamed from the storage without loading them into memory.
 *
 * The object is opened before responding, so that a missing object is answered with an error, instead of a
 * truncated successful response.
 */
suspend fun ApplicationCall.respondStoredFile(
    objectKey: String,
    size: Long,
    contentType: ContentType,
    lastModified: Instant?,
) {
    val input = try {
        withContext(Dispatchers.IO) { FileStorageProvider.current.open(objectKey) }
    } catch (e: StoredObjectNotFoundException) {
        logger.error("The contents of a file are missing from the storage: $objectKey", e)
        return respondText("The contents of the file are missing", status = HttpStatusCode.InternalServerError)
    }
    lastModified?.let { response.header(HttpHeaders.LastModified, GMTDate(it.toEpochMilliseconds()).toHttpDate()) }
    respondOutputStream(contentType, HttpStatusCode.OK, size) {
        withContext(Dispatchers.IO) {
            input.use { it.copyTo(this@respondOutputStream) }
        }
    }
}
