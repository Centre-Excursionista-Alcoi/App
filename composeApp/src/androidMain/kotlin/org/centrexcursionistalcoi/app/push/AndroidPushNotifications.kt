package org.centrexcursionistalcoi.app.push

import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import ceaapp.composeapp.generated.resources.*
import com.mmk.kmpnotifier.notification.configuration.NotificationPlatformConfiguration
import io.github.aakira.napier.Napier
import kotlin.reflect.KClass
import org.centrexcursionistalcoi.app.MainActivity
import org.centrexcursionistalcoi.app.R
import org.centrexcursionistalcoi.app.notifications.NotificationChannels
import org.centrexcursionistalcoi.app.push.payload.AdminUserPayload
import org.centrexcursionistalcoi.app.push.payload.BookingPayload
import org.jetbrains.compose.resources.getString

object AndroidPushNotifications {
    fun initialize(context: Context) {
        NotificationChannels.create(context)

        val showBookingConfirmedNotification = NotificationSender<BookingPayload> { payload ->
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

        val showBookingCancelledNotification = NotificationSender<BookingPayload> { payload ->
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

        val showUserRegisteredNotification = NotificationSender<AdminUserPayload> { payload ->
            Napier.i { "User registered: ${payload.userId}" }

            showNotification(
                context,
                NotificationChannels.CHANNEL_ID_REGISTRATION,
                payload.hashCode(),
                getString(Res.string.notification_user_registered_title),
                getString(Res.string.notification_user_registered_message),
                MainActivity::class to {
                    action = MainActivity.ACTION_USER_CONFIRMED
                    putExtra(MainActivity.EXTRA_USER_ID, payload.userId)
                }
            )
        }

        PushNotifications.initialize(
            configuration = NotificationPlatformConfiguration.Android(
                notificationIconResId = R.drawable.ic_launcher_foreground,
                showPushNotification = false,
            ),
            showBookingConfirmedNotification,
            showBookingCancelledNotification,
            showUserRegisteredNotification,
        )
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

}
