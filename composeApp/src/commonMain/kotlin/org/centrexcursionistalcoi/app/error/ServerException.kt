package org.centrexcursionistalcoi.app.error

import io.ktor.http.HttpMethod

open class ServerException(
    val code: Int?,
    val response: String,
    val path: String? = null,
    val method: HttpMethod? = null
): RuntimeException("Server responded with error\nMethod: $method\nPath: $path\nCode: $code\nResponse: $response")
