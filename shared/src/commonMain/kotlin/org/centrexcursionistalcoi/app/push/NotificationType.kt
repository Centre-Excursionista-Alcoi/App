package org.centrexcursionistalcoi.app.push

import kotlin.reflect.KClass
import org.centrexcursionistalcoi.app.push.payload.BookingConfirmedPayload
import org.centrexcursionistalcoi.app.push.payload.PushPayload

enum class NotificationType(
    val key: String,
    val payloadType: KClass<out PushPayload>
) {
    BookingConfirmed("b_confirmed", BookingConfirmedPayload::class)
}
