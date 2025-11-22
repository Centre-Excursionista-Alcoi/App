package org.centrexcursionistalcoi.app.data

import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.serializer.NullableUUIDSerializer

@Serializable
data class ReferencedInventoryItemType(
    override val id: Uuid,
    val displayName: String,
    val description: String?,
    val categories: List<String>?,
    val department: Department?,
    @Serializable(NullableUUIDSerializer::class) override val image: Uuid?,

    override val referencedEntity: InventoryItemType
): ReferencedEntity<Uuid, InventoryItemType>(), ImageFileContainer {
    companion object {
        fun InventoryItemType.referenced(departments: List<Department>) = ReferencedInventoryItemType(
            id = this.id,
            displayName = this.displayName,
            description = this.description,
            categories = this.categories,
            department = this.department?.let { deptId -> departments.firstOrNull { it.id == deptId } },
            image = this.image,
            referencedEntity = this
        )
    }

    override val files: Map<String, Uuid?> = mapOf("image" to image)

    override fun toString(): String = displayName
}
