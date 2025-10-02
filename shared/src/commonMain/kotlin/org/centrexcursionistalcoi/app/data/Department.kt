package org.centrexcursionistalcoi.app.data

import kotlinx.serialization.Serializable

@Serializable
data class Department(
    override val id: Long = 0L,
    val displayName: String,
    val imageFile: String? = null
): Entity<Long> {
    override fun toMap(): Map<String, Any?> {
        check(imageFile != null) { "File uploading is still not supported" }

        return mapOf(
            "id" to id.takeIf { it != 0L },
            "displayName" to displayName
        )
    }
}
