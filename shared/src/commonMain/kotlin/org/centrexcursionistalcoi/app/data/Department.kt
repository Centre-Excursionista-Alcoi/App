package org.centrexcursionistalcoi.app.data

import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable

@Serializable
data class Department(
    override val id: Long = 0L,
    val displayName: String,
    val imageFile: Uuid? = null
): Entity<Long>, FileContainer {
    override val files: Map<String, Uuid?> = mapOf("imageFile" to imageFile)

    override fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id.takeIf { it != 0L },
            "displayName" to displayName,
            "imageFile" to imageFile?.let { FileReference(it) }
        )
    }
}
