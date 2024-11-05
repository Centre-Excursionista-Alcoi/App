package org.centrexcursionistalcoi.app.error

open class ServerException(
    val code: Int?,
    val response: String
): RuntimeException("Server responded with error #$code: $response")
