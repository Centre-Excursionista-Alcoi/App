@file:UseSerializers(InstantSerializer::class)

package org.centrexcursionistalcoi.app.data

import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import org.centrexcursionistalcoi.app.serializer.InstantSerializer
import org.centrexcursionistalcoi.app.utils.isZero

@OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)
@Serializable
data class Post(
    override val id: Uuid,
    val date: Instant,
    val title: String,
    val content: String,
    val onlyForMembers: Boolean,
    val departmentId: Long,
): Entity<Uuid> {
    override fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id.takeIf { it.isZero() },
            "date" to date.toEpochMilliseconds(),
            "title" to title,
            "content" to content,
            "onlyForMembers" to onlyForMembers,
            "departmentId" to departmentId,
        )
    }
}
