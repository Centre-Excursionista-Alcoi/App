package org.centrexcursionistalcoi.app.request

import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable

@Serializable
data class UpdateInventoryItemRequest(
    val variation: String? = null,
    val type: Uuid? = null,
): UpdateEntityRequest {
    override fun isEmpty(): Boolean {
        return variation.isNullOrEmpty() && type == null
    }
}
