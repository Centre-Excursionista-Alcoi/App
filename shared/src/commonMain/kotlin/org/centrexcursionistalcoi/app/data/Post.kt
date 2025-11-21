@file:UseSerializers(InstantSerializer::class)

package org.centrexcursionistalcoi.app.data

import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import org.centrexcursionistalcoi.app.serializer.InstantSerializer
import org.centrexcursionistalcoi.app.utils.isZero

@Serializable
data class Post(
    override val id: Uuid,
    val date: Instant,
    val title: String,
    val content: String,
    val department: Uuid?,
    val link: String?,
    val files: List<Uuid>,
): Entity<Uuid> {
    override fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id.takeIf { it.isZero() },
            "date" to date.toEpochMilliseconds(),
            "title" to title,
            "content" to content,
            "department" to department,
            "link" to link,
            "files" to files,
        )
    }
}
