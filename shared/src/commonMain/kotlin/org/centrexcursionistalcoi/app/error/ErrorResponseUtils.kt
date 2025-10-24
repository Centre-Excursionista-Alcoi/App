package org.centrexcursionistalcoi.app.error

import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import org.centrexcursionistalcoi.app.json

/**
 * Tries to decode the body of the [HttpResponse] as an [Error] using the [ErrorSerializer].
 *
 * **[bodyAsText] must not have been called on the response before calling this function.**
 */
suspend fun HttpResponse.bodyAsError(): Error {
    val bodyText = bodyAsText()
    return json.decodeFromString(ErrorSerializer, bodyText)
}
