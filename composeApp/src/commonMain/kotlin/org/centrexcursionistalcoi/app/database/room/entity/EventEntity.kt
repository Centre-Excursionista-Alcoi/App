package org.centrexcursionistalcoi.app.database.room.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import org.centrexcursionistalcoi.app.data.Event
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Entity(tableName = "Events")
data class EventEntity(
    @PrimaryKey
    val id: Uuid,
    val start: Instant,
    val end: Instant?,
    val place: String,
    val title: String,
    val description: String?,
    val maxPeople: Long?,
    val requiresConfirmation: Boolean,
    val requiresInsurance: Boolean,
    val department: Uuid?,
    val image: Uuid?,
    val userReferences: List<String>,
) {
    fun toEvent() = Event(
        id = id,
        start = start,
        end = end,
        place = place,
        title = title,
        description = description,
        maxPeople = maxPeople,
        requiresConfirmation = requiresConfirmation,
        requiresInsurance = requiresInsurance,
        department = department,
        image = image,
        userSubList = userReferences,
    )

    companion object {
        fun Event.toEntity() = EventEntity(
            id = id,
            start = start,
            end = end,
            place = place,
            title = title,
            description = description,
            maxPeople = maxPeople,
            requiresConfirmation = requiresConfirmation,
            requiresInsurance = requiresInsurance,
            department = department,
            image = image,
            userReferences = userSubList,
        )
    }
}
