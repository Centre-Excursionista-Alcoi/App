package org.centrexcursionistalcoi.app.push

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import ceaapp.composeapp.generated.resources.*
import com.mmk.kmpnotifier.notification.NotifierManager
import com.mmk.kmpnotifier.notification.PayloadData
import com.mmk.kmpnotifier.notification.configuration.NotificationPlatformConfiguration
import io.github.aakira.napier.Napier
import kotlin.reflect.KClass
import kotlinx.coroutines.runBlocking
import org.centrexcursionistalcoi.app.MainActivity
import org.centrexcursionistalcoi.app.R
import org.centrexcursionistalcoi.app.network.AuthBackend
import org.centrexcursionistalcoi.app.notifications.NotificationChannels
import org.centrexcursionistalcoi.app.push.payload.BookingPayload
import org.centrexcursionistalcoi.app.serverJson
import org.jetbrains.compose.resources.getString

object PushNotifications {
    fun initialize(context: Context) {
        NotificationChannels.create(context)

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
                    runBlocking { AuthBackend.notifyToken(token) }
                }

                override fun onPayloadData(data: PayloadData) {
                    Napier.i { "Got new push notification" }
                    runBlocking {
                        decodePayload(context, data)
                    }
                }
            }
        )
    }

    suspend fun refreshTokenOnServer() {
        val token = NotifierManager.getPushNotifier().getToken()
        if (token == null) {
            Napier.i { "Tried to refresh token on server, but the device doesn't have a token." }
            return
        }
        Napier.d { "Notifying server about FCM token..." }
        if (!AuthBackend.notifyToken(token)) {
            Napier.w { "Server responded with an error to the FCM token update" }
        }
    }

    private suspend fun decodePayload(context: Context, payloadData: PayloadData) {
        if (
            ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Napier.i { "Won't show notification since permission is not granted." }
            return
        }

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
                showBookingConfirmedNotification(context, payload)
            }
            NotificationType.BookingCancelled -> {
                val payload = serverJson.decodeFromString(BookingPayload.serializer(), json)
                showBookingCancelledNotification(context, payload)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun showNotification(
        context: Context,
        channelId: String,
        notificationId: Int,
        title: String,
        text: String,
        action: Pair<KClass<out Activity>, Intent.() -> Unit>
    ) {
        val intent = Intent(context, action.first.java).apply { action.second(this) }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.cea_monochrome)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        with(NotificationManagerCompat.from(context)) {
            notify(notificationId, notification)
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun showBookingConfirmedNotification(context: Context, payload: BookingPayload) {
        Napier.i { "Booking confirmed: ${payload.bookingId}" }

        showNotification(
            context,
            NotificationChannels.CHANNEL_ID_CONFIRMATION,
            payload.hashCode(),
            getString(Res.string.notification_booking_confirmed_title),
            getString(Res.string.notification_booking_confirmed_message),
            MainActivity::class to {
                action = MainActivity.ACTION_BOOKING_CONFIRMED
                putExtra(MainActivity.EXTRA_BOOKING_ID, payload.bookingId)
                putExtra(MainActivity.EXTRA_BOOKING_TYPE, payload.bookingType)
            }
        )
    }

    @SuppressLint("MissingPermission")
    private suspend fun showBookingCancelledNotification(context: Context, payload: BookingPayload) {
        Napier.i { "Booking cancelled: ${payload.bookingId}" }

        showNotification(
            context,
            NotificationChannels.CHANNEL_ID_CANCELLATION,
            payload.hashCode(),
            getString(Res.string.notification_booking_cancelled_title),
            getString(Res.string.notification_booking_cancelled_message),
            MainActivity::class to {
                action = MainActivity.ACTION_BOOKING_CANCELLED
                putExtra(MainActivity.EXTRA_BOOKING_ID, payload.bookingId)
                putExtra(MainActivity.EXTRA_BOOKING_TYPE, payload.bookingType)
            }
        )
    }
}
