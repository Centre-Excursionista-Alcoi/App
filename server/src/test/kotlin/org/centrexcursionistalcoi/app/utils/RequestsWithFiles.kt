package org.centrexcursionistalcoi.app.utils

import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.content.TextContent
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.content.OutgoingContent
import io.ktor.http.content.PartData
import io.ktor.http.escapeIfNeeded
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonObject
import org.centrexcursionistalcoi.app.data.FileWithContext
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.request.FileRequestData
import org.centrexcursionistalcoi.app.request.ReceivedRequest
import org.centrexcursionistalcoi.app.request.RequestWithFiles

/**
 * The body the app sends for [request] (see its `requestBody`): JSON, or, if any of its files has contents,
 * multipart with each of them in a part of its own.
 */
fun <R : RequestWithFiles<R>> requestWithFilesBody(request: R, serializer: KSerializer<R>): OutgoingContent {
    val files = mutableListOf<Pair<String, FileWithContext>>()
    val withoutContents = request.mapFiles { file -> file.toPart(files) }
    return body(json.encodeToString(serializer, withoutContents), files)
}

/**
 * Like [requestWithFilesBody], for a request given as its fields' values, encoded with [toJsonElement].
 */
fun requestWithFilesBody(values: Map<String, Any?>): OutgoingContent {
    val files = mutableListOf<Pair<String, FileWithContext>>()
    val request = JsonObject(
        values.filterValues { it != null }.mapValues { (_, value) ->
            (if (value is FileWithContext) value.toPart(files) else value).toJsonElement()
        }
    )
    return body(json.encodeToString(JsonObject.serializer(), request), files)
}

/** Moves the contents of this file, if any, to a part of its own, added to [files]. */
private fun FileWithContext.toPart(files: MutableList<Pair<String, FileWithContext>>): FileWithContext {
    if (bytes.isEmpty()) return this
    val part = "file_${files.size}"
    files += part to this
    return copy(bytes = byteArrayOf(), part = part)
}

private fun body(request: String, files: List<Pair<String, FileWithContext>>): OutgoingContent {
    if (files.isEmpty()) return TextContent(request, ContentType.Application.Json)
    return MultiPartFormDataContent(
        formData {
            append(
                RequestWithFiles.REQUEST_PART,
                request,
                Headers.build { append(HttpHeaders.ContentType, ContentType.Application.Json.toString()) },
            )
            for ((part, file) in files) {
                append(
                    part,
                    file.bytes,
                    Headers.build {
                        append(HttpHeaders.ContentType, (file.contentType ?: ContentType.Application.OctetStream).toString())
                        append(HttpHeaders.ContentDisposition, "filename=${(file.name ?: part).escapeIfNeeded()}")
                    },
                )
            }
        }
    )
}

/** The part [withUploadedFile] uploads its file in. */
const val UPLOADED_PART = "file"

/**
 * Runs [block] as if the request being handled had uploaded [bytes] in the part named [UPLOADED_PART], for files
 * stored outside a request: a [FileWithContext] with that [part][FileWithContext.part] then refers to them.
 */
fun <T> withUploadedFile(bytes: ByteArray, block: () -> T): T = runBlocking {
    val upload = FileRequestData()
    upload.populate(PartData.FileItem({ ByteReadChannel(bytes) }, {}, Headers.Empty))
    try {
        ReceivedRequest(Unit, mapOf(UPLOADED_PART to upload)).withUploads { block() }
    } finally {
        upload.close()
    }
}
