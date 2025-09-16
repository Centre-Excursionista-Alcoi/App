package org.centrexcursionistalcoi.app.serialization

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonPrimitive

fun JsonObject.getString(key: String): String {
    return getValue(key).jsonPrimitive.content
}

fun JsonObject.getBoolean(key: String): Boolean {
    return getValue(key).jsonPrimitive.boolean
}
