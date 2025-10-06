package org.centrexcursionistalcoi.app.serializer

import kotlin.time.ExperimentalTime
import kotlin.uuid.Uuid
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@OptIn(ExperimentalTime::class)
object NullableUUIDSerializer : KSerializer<Uuid?> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("UUID", PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: Uuid?
    ) {
        encoder.encodeString(value?.toString() ?: "\u0000")
    }

    override fun deserialize(decoder: Decoder): Uuid? {
        val value = decoder.decodeString()
        return if (value == "\u0000") null else Uuid.parse(value)
    }
}
