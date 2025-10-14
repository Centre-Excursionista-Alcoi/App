package org.centrexcursionistalcoi.app.request

import kotlinx.serialization.Serializable

@Serializable
sealed interface UpdateEntityRequest {
    fun isEmpty(): Boolean
}
