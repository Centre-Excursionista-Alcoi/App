package org.centrexcursionistalcoi.app.server.response

import kotlinx.serialization.Serializable

@Serializable
sealed class ErrorResponse(
    val code: Int,
    val message: String,
    val httpStatusCode: Int = 400
): Response() {
    override val success: Boolean = false
}
