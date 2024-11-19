package org.centrexcursionistalcoi.app.notifications

import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import org.centrexcursionistalcoi.app.R

object NotificationChannels {
    const val GROUP_ID_BOOKINGS = "bookings"

    const val CHANNEL_ID_CONFIRMATION = "confirmation"

    @RequiresApi(Build.VERSION_CODES.O)
    private fun Context.createBookingsGroup(notificationManager: NotificationManager) {
        notificationManager.createNotificationChannelGroup(
            NotificationChannelGroup(GROUP_ID_BOOKINGS, getString(R.string.group_name_bookings_name)).apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    description = getString(R.string.group_name_bookings_description)
                }
            }
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun Context.bookingConfirmationsChannel(): NotificationChannel {
        val name = getString(R.string.channel_name_confirmation_name)
        val descriptionText = getString(R.string.channel_name_confirmation_description)
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        return NotificationChannel(CHANNEL_ID_CONFIRMATION, name, importance).apply {
            description = descriptionText
            group = GROUP_ID_BOOKINGS
        }
    }

    fun create(context: Context) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is not in the Support Library.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Register the channel with the system.
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            context.createBookingsGroup(notificationManager)

            notificationManager.createNotificationChannels(
                listOf(
                    context.bookingConfirmationsChannel()
                )
            )
        }
    }
}
