package org.centrexcursionistalcoi.app.authentik.errors

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject

object AuthentikErrorSerializer: JsonContentPolymorphicSerializer<AuthentikError>(AuthentikError::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<AuthentikError> {
        val jsonObject = element.jsonObject
        return when {
            jsonObject.containsKey("non_field_errors") -> Authentik403Error.serializer()
            jsonObject.containsKey("detail") -> Authentik400Error.serializer()
            else -> AuthentikError.serializer()
        }
    }
}
