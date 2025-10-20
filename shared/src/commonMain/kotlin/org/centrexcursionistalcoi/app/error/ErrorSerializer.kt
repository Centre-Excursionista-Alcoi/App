package org.centrexcursionistalcoi.app.error

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object ErrorSerializer : JsonContentPolymorphicSerializer<Error>(Error::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<Error> {
        val jsonObject = element.jsonObject
        val code = jsonObject["code"]?.jsonPrimitive?.intOrNull ?: throw IllegalArgumentException("Missing or invalid 'code' field in Error JSON object.")
        val serializer = Errors.serializer(code)
        return serializer ?: throw IllegalArgumentException("Unknown error code: $code")
    }
}
