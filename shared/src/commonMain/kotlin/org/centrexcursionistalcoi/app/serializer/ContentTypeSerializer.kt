package org.centrexcursionistalcoi.app.serializer

import io.ktor.http.ContentType
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object ContentTypeSerializer : KSerializer<ContentType> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("ContentType", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ContentType) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): ContentType = ContentType.parse(decoder.decodeString())
}
