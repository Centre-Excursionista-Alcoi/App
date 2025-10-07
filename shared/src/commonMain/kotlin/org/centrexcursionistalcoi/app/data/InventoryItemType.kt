package org.centrexcursionistalcoi.app.data

import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.serializer.NullableUUIDSerializer

@Serializable
data class InventoryItemType(
    override val id: Uuid,
    val displayName: String,
    val description: String?,
    @Serializable(NullableUUIDSerializer::class) override val image: Uuid?,
): Entity<Uuid>, ImageFileContainer {
    override val files: Map<String, Uuid?> = mapOf("image" to image)

    override fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "displayName" to displayName,
        "description" to description,
        "image" to image,
    )

    override fun toString(): String = displayName
}
