package org.centrexcursionistalcoi.app.request

import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.data.InventoryItem

@Serializable
data class UpdateInventoryItemRequest(
    val variation: String? = null,
    val type: Uuid? = null,
): UpdateEntityRequest<Uuid, InventoryItem> {
    override fun isEmpty(): Boolean {
        return variation.isNullOrEmpty() && type == null
    }
}
