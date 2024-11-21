package org.centrexcursionistalcoi.app.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.centrexcursionistalcoi.app.data.SpaceBookingD

@Entity
data class SpaceBooking(
    @PrimaryKey override val id: Int,
    override val createdAt: Instant,
    val spaceId: Int,
    override val userId: String,
    override val confirmed: Boolean,
    val keyId: Int? = null,
    override val from: LocalDate,
    override val to: LocalDate,
    override val takenAt: Instant? = null,
    override val returnedAt: Instant? = null,
    val paid: Boolean = false,
    val paymentReference: String? = null,
    val paymentDocument: ByteArray? = null
): BookingEntity<SpaceBookingD> {
    companion object : EntityDeserializer<SpaceBookingD, SpaceBooking> {
        override fun deserialize(source: SpaceBookingD): SpaceBooking {
            return SpaceBooking(
                id = source.id!!,
                createdAt = source.createdAt!!,
                spaceId = source.spaceId!!,
                userId = source.userId!!,
                confirmed = source.confirmed,
                keyId = source.keyId,
                from = source.from!!,
                to = source.to!!,
                takenAt = source.takenAt,
                returnedAt = source.returnedAt,
                paid = source.paid,
                paymentReference = source.paymentReference,
                paymentDocument = source.paymentDocument
            )
        }
    }

    override fun serializable(): SpaceBookingD {
        return SpaceBookingD(
            id = id.takeIf { it > 0 },
            createdAt = createdAt,
            spaceId = spaceId,
            userId = userId,
            confirmed = confirmed,
            keyId = keyId,
            from = from,
            to = to,
            takenAt = takenAt,
            returnedAt = returnedAt,
            paid = paid,
            paymentReference = paymentReference,
            paymentDocument = paymentDocument
        )
    }

    override fun validate(): Boolean {
        return true
    }
}
