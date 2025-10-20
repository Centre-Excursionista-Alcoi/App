package org.centrexcursionistalcoi.app.exception

class ServerException(
    message: String?,
    val responseStatusCode: Int?,
    val responseBody: String?,
    val errorCode: Int?
): Exception(message)
