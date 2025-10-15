package org.centrexcursionistalcoi.app.utils

import kotlin.io.encoding.Base64
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.JsonPrimitive

@OptIn(ExperimentalSerializationApi::class)
fun Any?.toJsonElement() = when (this) {
    null -> JsonPrimitive(null)
    is String -> JsonPrimitive(this)
    is Boolean -> JsonPrimitive(this)
    is Number -> JsonPrimitive(this)
    is ByteArray -> JsonPrimitive(Base64.UrlSafe.encode(this))
    else -> throw IllegalArgumentException("Cannot convert $this (${this::class.simpleName}) to JsonElement")
}
