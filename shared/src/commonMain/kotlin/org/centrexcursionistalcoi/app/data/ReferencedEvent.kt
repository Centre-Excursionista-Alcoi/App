package org.centrexcursionistalcoi.app.data

import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.serializer.InstantSerializer
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Serializable
data class ReferencedEvent(
    override val id: Uuid,
    @Serializable(InstantSerializer::class) val start: Instant,
    @Serializable(InstantSerializer::class) val end: Instant?,
    val place: String,
    val title: String,
    val description: String?,
    val maxPeople: Long?,
    val requiresConfirmation: Boolean,
    val requiresInsurance: Boolean,
    val department: Department?,
    override val image: Uuid?,

    /**
     * All the users that have confirmed assistance to the event.
     */
    val userSubList: List<UserData>,
): ReferencedEntity<Uuid, Event>, ImageFileContainer {
    companion object {
        fun Event.referenced(departments: List<Department>, users: List<UserData>) = ReferencedEvent(
            id = this.id,
            start = this.start,
            end = this.end,
            place = this.place,
            title = this.title,
            description = this.description,
            maxPeople = this.maxPeople,
            requiresConfirmation = this.requiresConfirmation,
            requiresInsurance = this.requiresInsurance,
            department = departments.firstOrNull { it.id == this.department },
            image = this.image,
            userSubList = users.filter { it.sub in this.userSubList },
        )
    }

    override fun dereference() = Event(
        id = id,
        start = start,
        end = end,
        place = place,
        title = title,
        description = description,
        maxPeople = maxPeople,
        requiresConfirmation = requiresConfirmation,
        requiresInsurance = requiresInsurance,
        department = department?.id,
        image = image,
        userSubList = userSubList.map { it.sub },
    )

    override val files: Map<String, Uuid?> = mapOf("image" to image)
}
