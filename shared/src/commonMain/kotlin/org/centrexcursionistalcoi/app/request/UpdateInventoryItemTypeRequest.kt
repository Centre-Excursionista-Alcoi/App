package org.centrexcursionistalcoi.app.request

import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.data.InventoryItemType
import org.centrexcursionistalcoi.app.serializer.Base64Serializer
import org.centrexcursionistalcoi.app.utils.isNullOrEmpty

@Serializable
data class UpdateInventoryItemTypeRequest(
    val displayName: String? = null,
    val description: String? = null,
    val categories: List<String>? = null,
    @Serializable(Base64Serializer::class) val image: ByteArray? = null,
): UpdateEntityRequest<Uuid, InventoryItemType> {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as UpdateInventoryItemTypeRequest

        if (displayName != other.displayName) return false
        if (description != other.description) return false
        if (categories != other.categories) return false
        if (!image.contentEquals(other.image)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = displayName.hashCode()
        result = 31 * result + (description?.hashCode() ?: 0)
        result = 31 * result + (categories?.hashCode() ?: 0)
        result = 31 * result + (image?.contentHashCode() ?: 0)
        return result
    }

    override fun isEmpty(): Boolean {
        return displayName.isNullOrEmpty() && description.isNullOrEmpty() && categories.isNullOrEmpty() && image.isNullOrEmpty()
    }
}
