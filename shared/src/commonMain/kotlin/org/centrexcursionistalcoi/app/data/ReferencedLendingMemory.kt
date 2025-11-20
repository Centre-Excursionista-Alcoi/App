package org.centrexcursionistalcoi.app.data

import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable

@Serializable
data class ReferencedLendingMemory(
    val place: String?,
    val memberUsers: List<UserData>,
    val externalUsers: String?,
    val text: String,
    val sport: Sports?,
    val department: Department?,
    val files: List<Uuid>,
) {
    fun dereference() = LendingMemory(
        place = place,
        memberUsers = memberUsers.map { it.sub },
        externalUsers = externalUsers,
        text = text,
        sport = sport,
        department = department?.id,
        files = files,
    )
}
