package org.centrexcursionistalcoi.app.server.response

import kotlinx.serialization.Serializable

@Serializable
open class SuccessResponse : Response() {
    override val success: Boolean = true
}
