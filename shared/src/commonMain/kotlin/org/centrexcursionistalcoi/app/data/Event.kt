package org.centrexcursionistalcoi.app.data

import kotlin.uuid.Uuid
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class Event(
    override val id: Uuid,
    val start: LocalDateTime,
    val end: LocalDateTime?,
    val place: String,
    val title: String,
    val description: String?,
    val maxPeople: Int?,
    val requiresConfirmation: Boolean,
    val department: Uuid?,
    override val image: Uuid?,

    /**
     * All the users that have confirmed assistance to the event.
     */
    val userSubList: List<String>,
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
        "department" to department,
        "image" to image,
        "userSubList" to userSubList,
    )
}
