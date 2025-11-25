package org.centrexcursionistalcoi.app.data

import kotlin.uuid.Uuid
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable

@Serializable
data class Event(
    override val id: Uuid,
    val date: LocalDate,
    val time: LocalTime?,
    val place: String,
    val title: String,
    val description: String?,
    val maxPeople: Int?,
    val requiresConfirmation: Boolean,
    val department: Uuid?,
    override val image: Uuid?,
): Entity<Uuid>, ImageFileContainer {
    override val files: Map<String, Uuid?> = mapOf("image" to image)

    override fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "date" to date,
        "time" to time,
        "place" to place,
        "title" to title,
        "description" to description,
        "maxPeople" to maxPeople,
        "requiresConfirmation" to requiresConfirmation,
        "department" to department,
        "image" to image,
    )
}
