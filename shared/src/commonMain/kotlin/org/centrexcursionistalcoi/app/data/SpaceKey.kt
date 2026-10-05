package org.centrexcursionistalcoi.app.data

import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.serializer.Base64Serializer
import kotlin.uuid.Uuid

/**
 * A key (or permit) the club has: an exact copy of a [SpaceKeyType], identifiable by its [id], and by an NFC tag if it
 * has one. When a lending takes keys, these are the ones handed out.
 */
@Serializable
data class SpaceKey(
    override val id: Uuid,
    val type: Uuid,
    /** How to tell it from the others of its type, e.g. a number written on it. */
    val label: String? = null,
    /** The id of the NFC tag attached to the key, if any. */
    @Serializable(Base64Serializer::class) val nfcId: ByteArray? = null,
) : Entity<Uuid> {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SpaceKey) return false

        return id == other.id && type == other.type && label == other.label && nfcId.contentEquals(other.nfcId)
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + (label?.hashCode() ?: 0)
        result = 31 * result + (nfcId?.contentHashCode() ?: 0)
        return result
    }
}
