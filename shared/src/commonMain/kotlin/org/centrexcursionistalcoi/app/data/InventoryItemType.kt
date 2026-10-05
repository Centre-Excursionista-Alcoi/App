package org.centrexcursionistalcoi.app.data

import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.exception.InventoryItemTypeNotFoundException
import org.centrexcursionistalcoi.app.serializer.NullableUUIDSerializer

@Serializable
data class InventoryItemType(
    override val id: Uuid,
    val displayName: String,
    val description: String?,
    val categories: List<String>?,
    val weight: Double? = null,
    val department: Uuid?,
    @Serializable(NullableUUIDSerializer::class) override val image: Uuid?,
): Entity<Uuid>, ImageFileContainer {
    companion object {
        /**
         * Gets an [InventoryItemType] from a list by its [id].
         * @throws InventoryItemTypeNotFoundException if no type with the given [id] is found.
         */
        fun List<InventoryItemType>.getType(id: Uuid): InventoryItemType = this.firstOrNull { it.id == id } ?: throw InventoryItemTypeNotFoundException(id)
    }

    override val files: Map<String, Uuid?> get() = mapOf("image" to image)

    override fun toString(): String = displayName
}
