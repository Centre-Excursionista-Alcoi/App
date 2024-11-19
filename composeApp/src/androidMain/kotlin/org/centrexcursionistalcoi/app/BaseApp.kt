package org.centrexcursionistalcoi.app

import android.app.Application
import com.mmk.kmpnotifier.notification.NotifierManager
import com.mmk.kmpnotifier.notification.PayloadData
import com.mmk.kmpnotifier.notification.configuration.NotificationPlatformConfiguration
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import kotlinx.serialization.json.Json
import org.centrexcursionistalcoi.app.auth.AccountManager
import org.centrexcursionistalcoi.app.notifications.NotificationChannels
import org.centrexcursionistalcoi.app.push.payload.BookingConfirmedPayload
import org.centrexcursionistalcoi.app.push.payload.PushPayload

class BaseApp : Application() {
    override fun onCreate() {
        super.onCreate()

        Napier.base(DebugAntilog())

        NotificationChannels.create(this)

        /**
         * By default, showPushNotification value is true.
         * When set showPushNotification to false foreground push notification will not be shown to user.
         * You can still get notification content using #onPushNotification listener method.
         */
        NotifierManager.initialize(
            configuration = NotificationPlatformConfiguration.Android(
                notificationIconResId = R.drawable.ic_launcher_foreground,
                showPushNotification = false,
            )
        )

        NotifierManager.addListener(
            object : NotifierManager.Listener {
                override fun onNewToken(token: String) {
                    Napier.i { "Got new FCM token: $token" }
                }

                override fun onPayloadData(data: PayloadData) {
                    Napier.i { "Got new push notification" }
                    val type = data["payload_type"] as String?
                    val json = data["data"] as String?
                    if (type == null || json == null) {
                        Napier.e { "Invalid push notification payload" }
                        return
                    }
                    when (val payload = Json.decodeFromString<PushPayload>(json)) {
                        is BookingConfirmedPayload -> {
                            payload.bookingId.let {
                                Napier.i { "Booking confirmed: $it" }
                            }
                        }
                    }
                }
            }
        )

        AccountManager.initialize(this)
    }

    override fun onTerminate() {
        super.onTerminate()

        AccountManager.close()
    }
}
