package org.centrexcursionistalcoi.app.utils

import kotlinx.serialization.json.JsonPrimitive

/** Converts the number into a [JsonPrimitive]. */
fun Number.toJson(): JsonPrimitive = JsonPrimitive(this)

/** Converts the string into a [JsonPrimitive]. */
fun String?.toJson(): JsonPrimitive = JsonPrimitive(this)
