package org.centrexcursionistalcoi.app.data

import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable

@Serializable
data class InventoryItemType(
    override val id: Uuid,
    val displayName: String,
    val description: String?,
    val image: Uuid?,
): Entity<Uuid>, FileContainer {
    override val files: Map<String, Uuid?> = mapOf("image" to image)

    override fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "displayName" to displayName,
        "description" to description,
        "image" to image,
    )
}
