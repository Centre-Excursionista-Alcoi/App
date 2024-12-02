package org.centrexcursionistalcoi.app.push.payload

import kotlinx.serialization.Serializable

@Serializable
data class BookingPayload(
    val bookingId: Int,
    val bookingType: BookingType
): PushPayload
