package org.centrexcursionistalcoi.app.data

import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
data class Memory(
    override val id: Uuid,
    val place: String?,
    val members: List<UInt>,
    val externalUsers: String?,
    val text: String,
    val sport: Sports?,
    val department: Uuid?,
    val attachments: List<Uuid>,
    val submittedBy: String,
    /** When the activity described by this memory started. For lending memories, this is the lending's [Lending.from]. */
    val from: ZonedDateTime,
    /** When the activity described by this memory ended. For lending memories, this is the lending's [Lending.to]. */
    val to: ZonedDateTime,
    val pdf: Uuid?,
    val lending: Uuid?,
): Entity<Uuid>, DocumentFileContainer, ImageFileListContainer {
    /** The generated summary PDF, exposed as a [DocumentFileContainer] so it's downloaded like any other document. */
    override val documentFile: Uuid? get() = pdf

    /** The user-attached photos, exposed as an [ImageFileListContainer] (fetched on demand, like [Post.images]). */
    override val images: List<Uuid> get() = attachments

    fun referenced(users: List<UserData>, members: List<Member>, departments: List<Department>) = ReferencedMemory(
        id = id,
        place = place,
        members = this.members.mapNotNull { memberNumber -> members.find { it.memberNumber == memberNumber } },
        externalUsers = externalUsers,
        text = text,
        sport = sport,
        department = departments.find { it.id == department },
        attachments = attachments,
        submittedBy = users.find { it.sub == submittedBy } ?: throw IllegalArgumentException("User with sub $submittedBy not found"),
        from = from,
        to = to,
        pdf = pdf,
        lending = lending,
    )
}
