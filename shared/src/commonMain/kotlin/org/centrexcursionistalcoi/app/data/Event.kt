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
    val userSubList: List<String>,

    /**
     * The qualifications a user must hold to confirm their assistance, as a conjunction of alternatives: the
     * user must satisfy **every** group (AND), and a group is satisfied by holding **any** of its qualifications
     * (OR). So `[[A], [B, C]]` reads `A AND (B OR C)`. Empty if the event has no requirements. See
     * [unmetRequirements].
     *
     * Only events that belong to a [department] can have requirements, and only on that department's own
     * qualifications.
     */
    val qualificationRequirements: List<List<Uuid>> = emptyList(),
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
        "userSubList" to userSubList,
        "qualificationRequirements" to qualificationRequirements,
    )
}
