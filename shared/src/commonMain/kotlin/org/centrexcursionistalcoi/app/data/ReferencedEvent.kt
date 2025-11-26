package org.centrexcursionistalcoi.app.data

import kotlin.uuid.Uuid
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class ReferencedEvent(
    override val id: Uuid,
    val start: LocalDateTime,
    val end: LocalDateTime?,
    val place: String,
    val title: String,
    val description: String?,
    val maxPeople: Long?,
    val requiresConfirmation: Boolean,
    val department: Department?,
    override val image: Uuid?,

    /**
     * All the users that have confirmed assistance to the event.
     */
    val usersList: List<UserData>,

    override val referencedEntity: Event
): ReferencedEntity<Uuid, Event>(), ImageFileContainer {
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
            department = departments.firstOrNull { it.id == this.department },
            image = this.image,
            usersList = users.filter { it.sub in this.userSubList },
            referencedEntity = this
        )
    }

    override val files: Map<String, Uuid?> = referencedEntity.files
}
