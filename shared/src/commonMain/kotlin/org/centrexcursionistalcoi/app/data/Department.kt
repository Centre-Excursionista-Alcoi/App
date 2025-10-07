package org.centrexcursionistalcoi.app.data

import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.serializer.NullableUUIDSerializer

@Serializable
data class Department(
    override val id: Int = 0,
    val displayName: String,
    @Serializable(NullableUUIDSerializer::class) override val image: Uuid? = null
): Entity<Int>, ImageFileContainer {
    override val files: Map<String, Uuid?> = mapOf("image" to image)

    override fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id.takeIf { it != 0 },
            "displayName" to displayName,
            "image" to image?.let { FileReference(it) }
        )
    }

    override fun toString(): String {
        return displayName
    }
}
