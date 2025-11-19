package org.centrexcursionistalcoi.app.data

import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.centrexcursionistalcoi.app.serializer.NullableUUIDSerializer

@Serializable
data class Department(
    override val id: Uuid,
    val displayName: String,
    @Serializable(NullableUUIDSerializer::class) override val image: Uuid? = null
) : Entity<Uuid>, ImageFileContainer {
    @Transient
    override val files: Map<String, Uuid?> = mapOf("image" to image)

    override fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "displayName" to displayName,
        "image" to image?.let { FileReference(it) }
    )

    override fun toString(): String {
        return displayName
    }
}
