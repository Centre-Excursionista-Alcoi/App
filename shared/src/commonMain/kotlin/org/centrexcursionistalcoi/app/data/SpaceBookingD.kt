package org.centrexcursionistalcoi.app.data

import kotlinx.serialization.Serializable

@Serializable
data class SpaceBookingD(
    override val id: Int? = null,
    val createdAt: Long? = null,
    val from: Long? = null,
    val to: Long? = null,
    val userId: String? = null,
    val spaceId: Int? = null,
    val confirmed: Boolean = false,
    val keyId: Int? = null,
    val takenAt: Long? = null,
    val returnedAt: Long? = null,
    val paid: Boolean = false,
    val paymentReference: String? = null,
    val paymentDocument: ByteArray? = null
): DatabaseData {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as SpaceBookingD

        if (id != other.id) return false
        if (createdAt != other.createdAt) return false
        if (from != other.from) return false
        if (to != other.to) return false
        if (spaceId != other.spaceId) return false
        if (confirmed != other.confirmed) return false
        if (keyId != other.keyId) return false
        if (takenAt != other.takenAt) return false
        if (returnedAt != other.returnedAt) return false
        if (paid != other.paid) return false
        if (userId != other.userId) return false
        if (paymentReference != other.paymentReference) return false
        if (paymentDocument != null) {
            if (other.paymentDocument == null) return false
            if (!paymentDocument.contentEquals(other.paymentDocument)) return false
        } else if (other.paymentDocument != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id ?: 0
        result = 31 * result + (createdAt?.hashCode() ?: 0)
        result = 31 * result + (from?.hashCode() ?: 0)
        result = 31 * result + (to?.hashCode() ?: 0)
        result = 31 * result + (spaceId ?: 0)
        result = 31 * result + confirmed.hashCode()
        result = 31 * result + (keyId ?: 0)
        result = 31 * result + (takenAt?.hashCode() ?: 0)
        result = 31 * result + (returnedAt?.hashCode() ?: 0)
        result = 31 * result + paid.hashCode()
        result = 31 * result + (userId?.hashCode() ?: 0)
        result = 31 * result + (paymentReference?.hashCode() ?: 0)
        result = 31 * result + (paymentDocument?.contentHashCode() ?: 0)
        return result
    }
}
