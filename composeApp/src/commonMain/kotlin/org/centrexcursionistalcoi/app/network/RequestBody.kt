package org.centrexcursionistalcoi.app.network

import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.content.OutgoingContent
import io.ktor.http.content.TextContent
import io.ktor.http.escapeIfNeeded
import kotlinx.serialization.KSerializer
import org.centrexcursionistalcoi.app.data.FileWithContext
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.request.RequestWithFiles

/**
 * The body to send [request] in: JSON, or, if it carries files with contents ([RequestWithFiles]), multipart,
 * with each file in a part of its own instead of encoded in the JSON.
 */
fun <R : Any> requestBody(request: R, serializer: KSerializer<R>): OutgoingContent {
    val files = mutableListOf<Pair<String, FileWithContext>>()
    val requestWithoutContents = if (request is RequestWithFiles<*>) {
        @Suppress("UNCHECKED_CAST")
        request.mapFiles { file ->
            if (file.bytes.isEmpty()) {
                file
            } else {
                val part = "file_${files.size}"
                files += part to file
                file.copy(bytes = byteArrayOf(), part = part)
            }
        } as R
    } else {
        request
    }
    if (files.isEmpty()) {
        return TextContent(json.encodeToString(serializer, request), ContentType.Application.Json)
    }
    return MultiPartFormDataContent(
        formData {
            // First, as the server tells multipart requests apart by it
            append(
                RequestWithFiles.REQUEST_PART,
                json.encodeToString(serializer, requestWithoutContents),
                Headers.build { append(HttpHeaders.ContentType, ContentType.Application.Json.toString()) },
            )
            for ((part, file) in files) {
                append(
                    part,
                    file.bytes,
                    Headers.build {
                        append(HttpHeaders.ContentType, (file.contentType ?: ContentType.Application.OctetStream).toString())
                        // The server streams only parts with a file name
                        append(HttpHeaders.ContentDisposition, "filename=${(file.name ?: part).escapeIfNeeded()}")
                    },
                )
            }
        }
    )
}
