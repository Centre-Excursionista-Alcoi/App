package org.centrexcursionistalcoi.app.server.response

import kotlinx.serialization.Serializable

@Serializable
sealed class Response {
    abstract val success: Boolean
}
