package org.centrexcursionistalcoi.app.data

import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
data class ReferencedLendingMemory(
    val place: String?,
    val members: List<Member>,
    val externalUsers: String?,
    val text: String,
    val sport: Sports?,
    val department: Department?,
    val files: List<Uuid>,
) {
    fun dereference() = LendingMemory(
        place = place,
        members = members.map { it.memberNumber },
        externalUsers = externalUsers,
        text = text,
        sport = sport,
        department = department?.id,
        files = files,
    )
}
