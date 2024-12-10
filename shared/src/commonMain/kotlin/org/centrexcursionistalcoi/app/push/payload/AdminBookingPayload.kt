package org.centrexcursionistalcoi.app.push.payload

import kotlinx.serialization.Serializable

@Serializable
class AdminBookingPayload(
    override val type: AdminNotificationType,

    /**
     * The ID string of the booking.
     */
    override val rawData: String
) : AdminPayload {
    val bookingIdString: String get() = rawData
}
