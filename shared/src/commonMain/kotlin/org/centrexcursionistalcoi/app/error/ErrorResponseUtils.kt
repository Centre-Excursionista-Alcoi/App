package org.centrexcursionistalcoi.app.error

import io.ktor.client.statement.*
import kotlinx.serialization.SerializationException
import org.centrexcursionistalcoi.app.json

/**
 * Tries to decode the body of the [HttpResponse] as an [Error] using the [ErrorPolymorphicSerializer].
 *
 * **[bodyAsText] must not have been called on the response before calling this function.**
 */
suspend fun HttpResponse.bodyAsError(): Error {
    val bodyText = bodyAsText()
    return try {
        json.decodeFromString(ErrorPolymorphicSerializer, bodyText)
    } catch (e: IllegalArgumentException) {
        // error from ErrorPolymorphicSerializer
        Error.SerializationError(e.message, bodyText, status)
    } catch (e: SerializationException) {
        // error with serialization; preserve the response's real status code instead of
        // defaulting to 500, since the body just didn't contain the Error JSON we expected
        Error.SerializationError(e.message, bodyText, status)
    }
}
