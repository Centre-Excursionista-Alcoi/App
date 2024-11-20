package org.centrexcursionistalcoi.app.push.payload

import kotlinx.serialization.Serializable

@Serializable
data class BookingConfirmedPayload(
    val bookingId: Int,
    val bookingType: String
): PushPayload
