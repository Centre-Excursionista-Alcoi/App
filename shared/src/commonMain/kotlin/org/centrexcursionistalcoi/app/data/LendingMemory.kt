package org.centrexcursionistalcoi.app.data

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlin.uuid.Uuid

@Serializable
data class LendingMemory(
    val place: String?,
    val members: List<UInt>,
    val externalUsers: String?,
    val text: String,
    val sport: Sports?,
    val department: Uuid?,
    val files: List<Uuid>,
): JsonSerializable {
    @OptIn(ExperimentalSerializationApi::class)
    override fun toJsonObject(): JsonObject = buildJsonObject {
        put("place", JsonPrimitive(place))
        put("members", JsonArray(members.map { JsonPrimitive(it) }))
        put("externalUsers", JsonPrimitive(externalUsers))
        put("text", JsonPrimitive(text))
        put("sport", JsonPrimitive(sport?.name))
        put("department", JsonPrimitive(department?.toString()))
        put("files", JsonArray(files.map { JsonPrimitive(it.toString()) }))
    }

    fun referenced(members: List<Member>, departments: List<Department>) = ReferencedLendingMemory(
        place = place,
        members = this.members.mapNotNull { memberNumber -> members.find { it.memberNumber == memberNumber } },
        externalUsers = externalUsers,
        text = text,
        sport = sport,
        department = departments.find { it.id == department },
        files = files,
    )
}
