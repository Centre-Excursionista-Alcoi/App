package org.centrexcursionistalcoi.app.serializer

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import org.centrexcursionistalcoi.app.data.PaymentPayload
import org.centrexcursionistalcoi.app.data.RedsysPayload

/**
 * Reads a [PaymentPayload] as its provider's own JSON, with no type tag, choosing the provider by the fields present
 * (see [JsonContentPolymorphicSerializer]). Writing needs nothing here: each subclass is `@Serializable` and is
 * written with its own serializer.
 *
 * To support a new provider, add its branch in [selectDeserializer] with a field only it has.
 */
object PaymentPayloadSerializer : JsonContentPolymorphicSerializer<PaymentPayload>(PaymentPayload::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<PaymentPayload> {
        val fields = (element as? JsonObject)?.keys ?: throw SerializationException("A payment payload must be a JSON object")
        return when {
            "Ds_Order" in fields -> RedsysPayload.serializer()
            else -> throw SerializationException("Unknown payment payload, fields: $fields")
        }
    }
}
