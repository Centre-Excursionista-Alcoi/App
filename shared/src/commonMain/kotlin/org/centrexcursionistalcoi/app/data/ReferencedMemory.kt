package org.centrexcursionistalcoi.app.data

import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
data class ReferencedMemory(
    override val id: Uuid,
    val place: String?,
    val members: List<Member>,
    val externalUsers: String?,
    val text: String,
    val sport: Sports?,
    val department: Department?,
    val pdf: Uuid?,
    val attachments: List<Uuid>,
    val submittedBy: UserData,
    val from: ZonedDateTime,
    val to: ZonedDateTime,
    val lending: Uuid? = null,
): ReferencedEntity<Uuid, Memory>, FileContainer, ImageFileListContainer {
    override fun dereference() = Memory(
        id = id,
        place = place,
        members = members.map { it.memberNumber },
        externalUsers = externalUsers,
        text = text,
        sport = sport,
        department = department?.id,
        attachments = attachments,
        submittedBy = submittedBy.sub,
        from = from,
        to = to,
        pdf = pdf,
        lending = lending,
    )

    override val files: Map<String, Uuid?> = mapOf(
        "pdf" to pdf
    )

    override val images: List<Uuid>
        get() = attachments
}
