package org.centrexcursionistalcoi.app.data

import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.serializer.InstantSerializer
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Serializable
data class Event(
    override val id: Uuid,
    @Serializable(InstantSerializer::class) val start: Instant,
    @Serializable(InstantSerializer::class) val end: Instant?,
    val place: String,
    val title: String,
    val description: String?,
    val maxPeople: Long?,
    val requiresConfirmation: Boolean,
    val requiresInsurance: Boolean,
    val department: Uuid?,
    override val image: Uuid?,

    /**
     * All the users that have confirmed assistance to the event.
     */
    val userReferences: List<String>,
): Entity<Uuid>, ImageFileContainer {
    override val files: Map<String, Uuid?> = mapOf("image" to image)

    override fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "start" to start,
        "end" to end,
        "place" to place,
        "title" to title,
        "description" to description,
        "maxPeople" to maxPeople,
        "requiresConfirmation" to requiresConfirmation,
        "requiresInsurance" to requiresInsurance,
        "department" to department,
        "image" to image,
        "userReferences" to userReferences,
    )
}
