package org.centrexcursionistalcoi.app.data

import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.serializer.InstantSerializer

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
    val department: Department?,
    override val image: Uuid?,

    /**
     * All the users that have confirmed assistance to the event.
     */
    val userReferences: List<UserData>,

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
            userReferences = users.filter { it.sub in this.userReferences },
            referencedEntity = this
        )
    }

    override val files: Map<String, Uuid?> = referencedEntity.files
}
