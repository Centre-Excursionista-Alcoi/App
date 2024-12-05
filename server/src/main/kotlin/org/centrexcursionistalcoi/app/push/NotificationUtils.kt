package org.centrexcursionistalcoi.app.push

import org.centrexcursionistalcoi.app.data.IBookingD
import org.centrexcursionistalcoi.app.database.common.BookingEntity
import org.centrexcursionistalcoi.app.database.entity.Lending
import org.centrexcursionistalcoi.app.database.entity.SpaceBooking
import org.centrexcursionistalcoi.app.push.payload.BookingType
import org.jetbrains.exposed.dao.IntEntityClass

object NotificationUtils {
    /**
     * Get the booking type of the entity class.
     */
    fun <Serializable : IBookingD, Entity : BookingEntity<Serializable>, EntityClass : IntEntityClass<Entity>> EntityClass.bookingType(): BookingType {
        return when (this::class) {
            Lending::class -> BookingType.Lending
            SpaceBooking::class -> BookingType.SpaceBooking
            else -> throw IllegalArgumentException("Unsupported booking type: ${this::class}")
        }
    }
}
