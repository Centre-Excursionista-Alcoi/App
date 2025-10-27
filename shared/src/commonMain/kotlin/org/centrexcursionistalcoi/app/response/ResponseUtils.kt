package org.centrexcursionistalcoi.app.response

import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.DeserializationStrategy
import org.centrexcursionistalcoi.app.json

/**
 * Tries to decode the body of the [HttpResponse] as a [T].
 *
 * **[bodyAsText] must not have been called on the response before calling this function.**
 *
 * @param deserializationStrategy The deserialization strategy to use for decoding the response body.
 * @return The decoded body of the response as a [T].
 *
 * @throws kotlinx.serialization.SerializationException if the given JSON string is not a valid JSON input for the type [T].
 * @throws IllegalArgumentException if the decoded input cannot be represented as a valid instance of type [T].
 */
suspend fun <T> HttpResponse.bodyAsJson(deserializationStrategy: DeserializationStrategy<T>): T {
    val body = bodyAsText()
    return json.decodeFromString(deserializationStrategy, body)
}
