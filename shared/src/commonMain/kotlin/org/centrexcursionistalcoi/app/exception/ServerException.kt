package org.centrexcursionistalcoi.app.exception

import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.SerializationException
import org.centrexcursionistalcoi.app.error.Error
import org.centrexcursionistalcoi.app.json

class ServerException(
    message: String?,
    val responseStatusCode: Int?,
    val responseBody: String?,
    val errorCode: Int?
): Exception(message) {
    companion object {
        suspend fun fromResponse(httpResponse: HttpResponse): ServerException {
            val body = httpResponse.bodyAsText()
            val error = try {
                json.decodeFromString(Error.serializer(), body)
            } catch (_: SerializationException) {
                null
            }
            return ServerException(error?.description ?: "Unknown server error", httpResponse.status.value, body, error?.code)
        }
    }
}
