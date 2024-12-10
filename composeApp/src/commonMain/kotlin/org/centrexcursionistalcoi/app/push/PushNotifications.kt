package org.centrexcursionistalcoi.app.push

import com.mmk.kmpnotifier.notification.NotifierManager
import com.mmk.kmpnotifier.notification.PayloadData
import com.mmk.kmpnotifier.notification.configuration.NotificationPlatformConfiguration
import io.github.aakira.napier.Napier
import kotlinx.coroutines.runBlocking
import kotlinx.io.IOException
import org.centrexcursionistalcoi.app.data.enumeration.NotificationType
import org.centrexcursionistalcoi.app.network.AuthBackend
import org.centrexcursionistalcoi.app.push.payload.AdminUserPayload
import org.centrexcursionistalcoi.app.push.payload.BookingPayload
import org.centrexcursionistalcoi.app.serverJson

object PushNotifications {
    private var showBookingConfirmedNotification: NotificationSender<BookingPayload>? = null
    private var showBookingCancelledNotification: NotificationSender<BookingPayload>? = null
    private var showUserRegisteredNotification: NotificationSender<AdminUserPayload>? = null

    fun initialize(
        configuration: NotificationPlatformConfiguration,
        showBookingConfirmedNotification: NotificationSender<BookingPayload>,
        showBookingCancelledNotification: NotificationSender<BookingPayload>,
        showUserRegisteredNotification: NotificationSender<AdminUserPayload>
    ) {
        this.showBookingConfirmedNotification = showBookingConfirmedNotification
        this.showBookingCancelledNotification = showBookingCancelledNotification
        this.showUserRegisteredNotification = showUserRegisteredNotification

        /**
         * By default, showPushNotification value is true.
         * When set showPushNotification to false foreground push notification will not be shown to user.
         * You can still get notification content using #onPushNotification listener method.
         */
        NotifierManager.initialize(configuration)

        NotifierManager.addListener(
            object : NotifierManager.Listener {
                override fun onNewToken(token: String) {
                    Napier.i { "Got new FCM token: $token" }
                    runBlocking { AuthBackend.notifyToken(token) }
                }

                override fun onPayloadData(data: PayloadData) {
                    Napier.i { "Got new push notification" }
                    runBlocking {
                        decodePayload(data)
                    }
                }
            }
        )
    }

    suspend fun refreshTokenOnServer() {
        try {
            val token = NotifierManager.getPushNotifier().getToken()
            if (token == null) {
                Napier.i { "Tried to refresh token on server, but the device doesn't have a token." }
                return
            }
            Napier.d { "Notifying server about FCM token..." }
            if (!AuthBackend.notifyToken(token)) {
                Napier.w { "Server responded with an error to the FCM token update" }
            }
        } catch (e: IOException) {
            Napier.e(e) { "Could not get FCM token." }
        }
    }

    private suspend fun decodePayload(payloadData: PayloadData) {
        val typeValue = payloadData["type"] as String?
        val json = payloadData["data"] as String?
        if (typeValue == null || json == null) {
            Napier.e { "Invalid push notification payload" }
            return
        }
        Napier.d { "Payload type: $typeValue" }
        val type = try {
            NotificationType.valueOf(typeValue)
        } catch (_: IllegalArgumentException) {
            Napier.e { "Invalid notification type: $typeValue" }
            return
        }
        when (type) {
            NotificationType.BookingConfirmed -> {
                val payload = serverJson.decodeFromString(BookingPayload.serializer(), json)
                showBookingConfirmedNotification!!(payload)
            }
            NotificationType.BookingCancelled -> {
                val payload = serverJson.decodeFromString(BookingPayload.serializer(), json)
                showBookingCancelledNotification!!(payload)
            }
            NotificationType.UserRegistered -> {
                val payload = serverJson.decodeFromString(AdminUserPayload.serializer(), json)
                showUserRegisteredNotification!!(payload)
            }
        }
    }
}