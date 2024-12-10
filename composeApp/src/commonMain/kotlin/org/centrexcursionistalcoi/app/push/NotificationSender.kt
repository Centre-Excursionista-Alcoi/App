package org.centrexcursionistalcoi.app.push

import org.centrexcursionistalcoi.app.push.payload.PushPayload

fun interface NotificationSender<PayloadType : PushPayload> {
    suspend operator fun invoke(payload: PayloadType)
}
