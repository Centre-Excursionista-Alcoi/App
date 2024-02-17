package backend.data.user

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

enum class Role {
    /**
     * Allows creating, editing and removing inventory items. As well as confirming lends.
     */
    INVENTORY_MANAGER;

    object Serializer : KSerializer<Role> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("Role", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): Role {
            val key = decoder.decodeString()
            return valueOf(key)
        }

        override fun serialize(encoder: Encoder, value: Role) {
            encoder.encodeString(value.name)
        }
    }
}
