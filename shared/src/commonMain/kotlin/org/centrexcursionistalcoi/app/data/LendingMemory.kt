package org.centrexcursionistalcoi.app.data

import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

@Serializable
data class LendingMemory(
    val place: String?,
    val memberUsers: List<String>,
    val externalUsers: String?,
    val text: String,
    val files: List<Uuid>,
): JsonSerializable {
    override fun toJsonObject(): JsonObject = buildJsonObject {
        put("place", JsonPrimitive(place))
        put("memberUsers", JsonArray(memberUsers.map { JsonPrimitive(it) }))
        put("externalUsers", JsonPrimitive(externalUsers))
        put("text", JsonPrimitive(text))
        put("files", JsonArray(files.map { JsonPrimitive(it.toString()) }))
    }
}
