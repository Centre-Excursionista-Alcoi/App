package org.centrexcursionistalcoi.app.serialization

import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json

suspend fun <T> HttpResponse.bodyAsJson(serializer: DeserializationStrategy<T>, json: Json = org.centrexcursionistalcoi.app.plugins.json): T {
    val text = bodyAsText()
    return json.decodeFromString(serializer, text)
}
