package org.centrexcursionistalcoi.app.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
sealed interface JsonSerializable {
    fun toJsonObject(): JsonObject
}
