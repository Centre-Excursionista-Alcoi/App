package org.centrexcursionistalcoi.app.data

import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
data class ReferencedLendingMemory(
    val id: Uuid,
    val place: String?,
    val members: List<Member>,
    val externalUsers: String?,
    val text: String,
    val sport: Sports?,
    val department: Department?,
    val attachments: List<Uuid>,
    val submittedBy: String,
    val from: ZonedDateTime,
    val to: ZonedDateTime,
) {
    fun dereference() = Memory(
        id = id,
        place = place,
        members = members.map { it.memberNumber },
        externalUsers = externalUsers,
        text = text,
        sport = sport,
        department = department?.id,
        attachments = attachments,
        submittedBy = submittedBy,
        from = from,
        to = to,
    )
}
