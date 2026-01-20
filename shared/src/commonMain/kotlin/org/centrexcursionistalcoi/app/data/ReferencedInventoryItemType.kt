package org.centrexcursionistalcoi.app.data

import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.exception.InventoryItemTypeNotFoundException
import org.centrexcursionistalcoi.app.serializer.NullableUUIDSerializer
import kotlin.uuid.Uuid

@Serializable
data class ReferencedInventoryItemType(
    override val id: Uuid,
    val displayName: String,
    val description: String?,
    val categories: List<String>?,
    val weight: Double?,
    val department: Department?,
    @Serializable(NullableUUIDSerializer::class) override val image: Uuid?,

    override val referencedEntity: InventoryItemType
): ReferencedEntity<Uuid, InventoryItemType>(), ImageFileContainer {
    companion object {
        /**
         * Gets an [ReferencedInventoryItemType] from a list by its [id].
         * @throws InventoryItemTypeNotFoundException if no type with the given [id] is found.
         */
        fun List<ReferencedInventoryItemType>.getType(id: Uuid): ReferencedInventoryItemType = this.firstOrNull { it.id == id } ?: throw InventoryItemTypeNotFoundException(id)

        fun InventoryItemType.referenced(departments: List<Department>) = ReferencedInventoryItemType(
            id = this.id,
            displayName = this.displayName,
            description = this.description,
            categories = this.categories,
            weight = this.weight,
            department = this.department?.let { deptId -> departments.firstOrNull { it.id == deptId } },
            image = this.image,
            referencedEntity = this
        )
    }

    override val files: Map<String, Uuid?> = mapOf("image" to image)

    override fun toString(): String = displayName
}
