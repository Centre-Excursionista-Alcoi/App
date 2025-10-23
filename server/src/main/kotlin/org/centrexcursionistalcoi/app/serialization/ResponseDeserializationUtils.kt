package org.centrexcursionistalcoi.app.serialization

import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

suspend fun <T> HttpResponse.bodyAsJson(serializer: DeserializationStrategy<T>, json: Json = org.centrexcursionistalcoi.app.json): T {
    val text = bodyAsText()
    try {
        return json.decodeFromString(serializer, text)
    } catch(e: SerializationException) {
        throw SerializationException("Failed to deserialize response body: $text", e)
    }
}
