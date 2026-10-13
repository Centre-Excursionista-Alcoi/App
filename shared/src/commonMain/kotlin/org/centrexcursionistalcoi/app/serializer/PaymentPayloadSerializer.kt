package org.centrexcursionistalcoi.app.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import org.centrexcursionistalcoi.app.data.PaymentPayload
import org.centrexcursionistalcoi.app.data.RedsysPayload

/**
 * Serializes a [PaymentPayload] as its provider's own JSON, with no type tag, and picks the provider when reading by
 * the fields present. To support a new provider, add its branch in [select] with a field only it has.
 */
object PaymentPayloadSerializer : KSerializer<PaymentPayload> {
    override val descriptor: SerialDescriptor = JsonObject.serializer().descriptor

    private fun select(element: JsonObject): KSerializer<out PaymentPayload> = when {
        "Ds_Order" in element -> RedsysPayload.serializer()
        else -> throw SerializationException("Unknown payment payload, fields: ${element.keys}")
    }

    override fun deserialize(decoder: Decoder): PaymentPayload {
        val input = decoder as? JsonDecoder ?: throw SerializationException("PaymentPayload can only be read from JSON")
        val element = input.decodeJsonElement() as? JsonObject
            ?: throw SerializationException("A payment payload must be a JSON object")
        return input.json.decodeFromJsonElement(select(element), element)
    }

    override fun serialize(encoder: Encoder, value: PaymentPayload) {
        val output = encoder as? JsonEncoder ?: throw SerializationException("PaymentPayload can only be written as JSON")
        @Suppress("UNCHECKED_CAST")
        val serializer = when (value) {
            is RedsysPayload -> RedsysPayload.serializer()
        } as KSerializer<PaymentPayload>
        output.encodeJsonElement(output.json.encodeToJsonElement(serializer, value))
    }
}
