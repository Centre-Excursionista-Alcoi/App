package org.centrexcursionistalcoi.app.error

import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
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
        Error.SerializationError(e.message, bodyText)
    } catch (e: SerializationException) {
        // error with serialization
        Error.SerializationError(e.message, bodyText)
    } catch (e: SerializationException) {
        // error with serialization
        Error.Exception(e)
    }
}
