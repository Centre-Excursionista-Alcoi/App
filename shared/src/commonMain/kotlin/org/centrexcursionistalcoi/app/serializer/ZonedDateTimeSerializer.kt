package org.centrexcursionistalcoi.app.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.centrexcursionistalcoi.app.data.ZonedDateTime

/**
 * Encodes the ZonedDateTime as a string in RFC 9557.
 *
 * Example: `2026-09-02T12:58:55+02:00[Europe/Madrid]`
 */
object ZonedDateTimeSerializer : KSerializer<ZonedDateTime> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
        "ZonedDateTime",
        PrimitiveKind.STRING
    )

    override fun serialize(
        encoder: Encoder,
        value: ZonedDateTime
    ) {
        val formattedString = value.toString()
        encoder.encodeString(formattedString)
    }

    override fun deserialize(decoder: Decoder): ZonedDateTime {
        return ZonedDateTime.parse(decoder.decodeString())
    }
}
