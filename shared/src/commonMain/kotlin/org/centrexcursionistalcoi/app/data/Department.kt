package org.centrexcursionistalcoi.app.data

import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.serializer.NullableUUIDSerializer

@Serializable
data class Department(
    override val id: Int = 0,
    val displayName: String,
    @Serializable(NullableUUIDSerializer::class) val imageFile: Uuid? = null
): Entity<Int>, FileContainer {
    override val files: Map<String, Uuid?> = mapOf("image" to imageFile)

    override fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id.takeIf { it != 0 },
            "displayName" to displayName,
            "image" to imageFile?.let { FileReference(it) }
        )
    }

    override fun toString(): String {
        return displayName
    }
}
