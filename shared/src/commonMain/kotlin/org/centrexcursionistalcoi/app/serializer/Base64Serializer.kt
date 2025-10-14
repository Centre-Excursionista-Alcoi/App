package org.centrexcursionistalcoi.app.serializer

import kotlin.io.encoding.Base64
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object Base64Serializer : KSerializer<ByteArray> {
    private val base64 = Base64.UrlSafe

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Base64", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ByteArray) {
        encoder.encodeString(base64.encode(value))
    }

    override fun deserialize(decoder: Decoder): ByteArray {
        val raw = decoder.decodeString()
        return base64.decode(raw)
    }
}
