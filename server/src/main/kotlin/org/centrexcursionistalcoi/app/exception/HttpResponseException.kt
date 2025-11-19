package org.centrexcursionistalcoi.app.exception

class HttpResponseException(
    message: String,
    val statusCode: Int,
    val body: String?,
): RuntimeException(message)
