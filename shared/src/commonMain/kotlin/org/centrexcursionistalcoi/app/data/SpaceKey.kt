package org.centrexcursionistalcoi.app.data

import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.serializer.Base64Serializer
import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * A type of key a [Space] needs, e.g. the main door or the car access. A lending can take up to [maxQuantity] of them.
 */
@Serializable
data class SpaceKey(
    override val id: Uuid,
    val lastUpdate: Instant,
    val space: Uuid,
    val name: String,
    val maxQuantity: Int,
    /** The id of the NFC tag attached to the key, if any. */
    @Serializable(Base64Serializer::class) val nfcId: ByteArray? = null,
) : Entity<Uuid> {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SpaceKey) return false

        if (maxQuantity != other.maxQuantity) return false
        if (id != other.id) return false
        if (lastUpdate != other.lastUpdate) return false
        if (space != other.space) return false
        if (name != other.name) return false
        if (!nfcId.contentEquals(other.nfcId)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = maxQuantity
        result = 31 * result + id.hashCode()
        result = 31 * result + lastUpdate.hashCode()
        result = 31 * result + space.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + (nfcId?.contentHashCode() ?: 0)
        return result
    }
}
