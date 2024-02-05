package utils

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.encodeToJsonElement

/**
 * Puts the given [value] at [key] after encoding it with [Json].
 */
inline fun <reified Type> JsonObjectBuilder.putEncoded(key: String, value: Type) {
    put(key, Json.encodeToJsonElement(value))
}
