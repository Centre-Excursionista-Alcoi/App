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
    override val referencedEntity: Memory
): ReferencedEntity<Uuid, Memory>(), FileContainer, ImageFileListContainer {
    override val files: Map<String, Uuid?> = mapOf(
        "pdf" to pdf
    )

    override val images: List<Uuid>
        get() = attachments
}
