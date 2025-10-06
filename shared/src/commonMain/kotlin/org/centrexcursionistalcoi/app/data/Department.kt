package org.centrexcursionistalcoi.app.data

import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.serializer.NullableUUIDSerializer

@Serializable
data class Department(
    override val id: Long = 0L,
    val displayName: String,
    @Serializable(NullableUUIDSerializer::class) val imageFile: Uuid? = null
): Entity<Long>, FileContainer {
    override val files: Map<String, Uuid?> = mapOf("image" to imageFile)

    override fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id.takeIf { it != 0L },
            "displayName" to displayName,
            "image" to imageFile?.let { FileReference(it) }
        )
    }

    override fun toString(): String {
        return displayName
    }
}
