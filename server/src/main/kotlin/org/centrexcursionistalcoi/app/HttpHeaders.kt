package org.centrexcursionistalcoi.app

import io.ktor.http.HttpHeaders
import io.ktor.server.request.ApplicationRequest
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

val HttpHeaders.CEAInfo: String get() = "CEA-Info"

val HttpHeaders.CEAWebDAVMessage: String get() = "CEA-WebDAV-Message"

/**
 * Parses the 'If-Modified-Since' header from the request and returns it as an [Instant], or null if the header is not present.
 *
 * Syntax:
 * ```
 * If-Modified-Since: <day-name>, <day> <month> <year> <hour>:<minute>:<second> GMT
 * ```
 *
 * Example:
 * ```
 * If-Modified-Since: Wed, 21 Oct 2015 07:28:00 GMT
 * ```
 *
 * See: [`Mozilla Docs`](https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Headers/If-Modified-Since)
 */
fun ApplicationRequest.ifModifiedSince(): ZonedDateTime? {
    val header = this.headers[HttpHeaders.IfModifiedSince] ?: return null
    val formatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'")
    return try {
        LocalDateTime.parse(header, formatter).atZone(ZoneOffset.UTC)
    } catch (_: DateTimeParseException) {
        null
    }
}
