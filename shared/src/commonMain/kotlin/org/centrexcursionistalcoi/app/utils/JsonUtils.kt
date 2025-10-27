package org.centrexcursionistalcoi.app.utils

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

/** Converts the number into a [JsonPrimitive]. */
fun Number.toJson(): JsonPrimitive = JsonPrimitive(this)

/** Converts the string into a [JsonPrimitive]. */
fun String?.toJson(): JsonPrimitive = JsonPrimitive(this)

/** Converts the iterable into a Json Array. */
fun <T> Iterable<T>.toJson(serializer: (T) -> JsonElement): JsonArray = JsonArray(map(serializer))
