package org.centrexcursionistalcoi.app.data

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
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
    val pdf: Uuid? = null,
    val lending: Uuid? = null,
): JsonSerializable, Entity<Uuid>, FileContainer, ImageFileListContainer {
    /** The generated summary PDF, exposed as a [FileContainer] so it's downloaded like any other single document. */
    override val files: Map<String, Uuid?> = mapOf("pdf" to pdf)

    /** The user-attached photos, exposed as an [ImageFileListContainer] (fetched on demand, like [Post.images]). */
    override val images: List<Uuid> = attachments

    override fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "place" to place,
        "members" to members,
        "externalUsers" to externalUsers,
        "text" to text,
        "sport" to sport?.name,
        "department" to department,
        "attachments" to attachments,
        "submittedBy" to submittedBy,
        "from" to from,
        "to" to to,
        "pdf" to pdf,
        "lending" to lending,
    )

    @OptIn(ExperimentalSerializationApi::class)
    override fun toJsonObject(): JsonObject = buildJsonObject {
        put("id", JsonPrimitive(id.toString()))
        put("place", JsonPrimitive(place))
        put("members", JsonArray(members.map { JsonPrimitive(it) }))
        put("externalUsers", JsonPrimitive(externalUsers))
        put("text", JsonPrimitive(text))
        put("sport", JsonPrimitive(sport?.name))
        put("department", JsonPrimitive(department?.toString()))
        put("attachments", JsonArray(attachments.map { JsonPrimitive(it.toString()) }))
        put("submittedBy", JsonPrimitive(submittedBy))
        put("from", JsonPrimitive(from.toString()))
        put("to", JsonPrimitive(to.toString()))
        put("pdf", JsonPrimitive(pdf?.toString()))
        put("lending", JsonPrimitive(lending?.toString()))
    }

    fun referenced(members: List<Member>, departments: List<Department>) = ReferencedLendingMemory(
        id = id,
        place = place,
        members = this.members.mapNotNull { memberNumber -> members.find { it.memberNumber == memberNumber } },
        externalUsers = externalUsers,
        text = text,
        sport = sport,
        department = departments.find { it.id == department },
        attachments = attachments,
        submittedBy = submittedBy,
        from = from,
        to = to,
    )
}
