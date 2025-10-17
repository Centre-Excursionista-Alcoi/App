package org.centrexcursionistalcoi.app.request

import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.serializer.Base64Serializer

@Serializable
data class UpdateDepartmentRequest(
    val displayName: String? = null,
    @Serializable(Base64Serializer::class) val image: ByteArray? = null,
): UpdateEntityRequest<Int, Department> {
    override fun isEmpty(): Boolean {
        return displayName.isNullOrEmpty() && (image == null || image.isEmpty())
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as UpdateDepartmentRequest

        if (displayName != other.displayName) return false
        if (!image.contentEquals(other.image)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = displayName?.hashCode() ?: 0
        result = 31 * result + (image?.contentHashCode() ?: 0)
        return result
    }
}
