package org.centrexcursionistalcoi.app.serializer

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import org.centrexcursionistalcoi.app.data.PaymentReference
import org.centrexcursionistalcoi.app.data.RedsysReference

/**
 * Reads a [PaymentReference] choosing the provider by the fields present, with no type tag (see
 * [PaymentPayloadSerializer]). To support a new provider, add its branch with a field only it has.
 */
object PaymentReferenceSerializer : JsonContentPolymorphicSerializer<PaymentReference>(PaymentReference::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<PaymentReference> {
        val fields = (element as? JsonObject)?.keys ?: throw SerializationException("A payment reference must be a JSON object")
        return when {
            "redsys_order" in fields -> RedsysReference.serializer()
            else -> throw SerializationException("Unknown payment reference, fields: $fields")
        }
    }
}
