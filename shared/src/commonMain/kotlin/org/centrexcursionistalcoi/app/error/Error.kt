package org.centrexcursionistalcoi.app.error

import io.ktor.http.HttpStatusCode
import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.exception.ServerException
import org.centrexcursionistalcoi.app.serializer.HttpStatusCodeSerializer

@Serializable
sealed class Error(
    val code: Int,
    val description: String? = null,
    @Serializable(HttpStatusCodeSerializer::class) val statusCode: HttpStatusCode = HttpStatusCode.BadRequest,
) {
    fun toThrowable(): ServerException {
        return ServerException(
            description,
            responseStatusCode = statusCode.value,
            responseBody = null,
            errorCode = code,
        )
    }
}
