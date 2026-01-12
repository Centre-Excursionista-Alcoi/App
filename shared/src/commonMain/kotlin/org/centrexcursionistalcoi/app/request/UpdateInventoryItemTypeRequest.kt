package org.centrexcursionistalcoi.app.request

import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.data.FileWithContext
import org.centrexcursionistalcoi.app.data.FileWithContext.Companion.isNullOrEmpty
import org.centrexcursionistalcoi.app.data.InventoryItemType

@Serializable
data class UpdateInventoryItemTypeRequest(
    val displayName: String? = null,
    val description: String? = null,
    val categories: List<String>? = null,
    val weight: Double? = null,
    val department: Uuid? = null,
    val image: FileWithContext? = null,
): UpdateEntityRequest<Uuid, InventoryItemType> {
    override fun isEmpty(): Boolean {
        return displayName.isNullOrEmpty() && description == null && weight == null && categories.isNullOrEmpty() && image.isNullOrEmpty() && department == null
    }
}
