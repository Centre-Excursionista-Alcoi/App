package org.centrexcursionistalcoi.app.utils

import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import java.time.LocalTime
import kotlin.uuid.Uuid
import kotlin.io.encoding.Base64
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.JsonPrimitive
import org.centrexcursionistalcoi.app.data.FileWithContext
import org.centrexcursionistalcoi.app.json

@OptIn(ExperimentalSerializationApi::class)
fun Any?.toJsonElement() = when (this) {
    null -> JsonPrimitive(null)
    is String -> JsonPrimitive(this)
    is Boolean -> JsonPrimitive(this)
    is Number -> JsonPrimitive(this)
    is ByteArray -> JsonPrimitive(Base64.UrlSafe.encode(this))
    is Uuid -> JsonPrimitive(this.toString())
    is LocalDate -> JsonPrimitive(this.toString())
    is LocalTime -> JsonPrimitive(this.toString())
    is Instant -> JsonPrimitive(this.toEpochMilliseconds())
    // Converts to FileWithContext
    is FileWithContext -> json.encodeToJsonElement(FileWithContext.serializer(), this)
    else -> throw IllegalArgumentException("Cannot convert $this (${this::class.simpleName}) to JsonElement")
}
