package org.centrexcursionistalcoi.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.mmk.kmpnotifier.permission.permissionUtil
import io.github.vinceglb.filekit.core.FileKit
import org.centrexcursionistalcoi.app.route.Loading
import org.centrexcursionistalcoi.app.route.Reservation
import org.centrexcursionistalcoi.app.route.Route

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FileKit.init(this)

        // this will ask permission in Android 13(API Level 33) or above, otherwise permission will be granted.
        val permissionUtil by permissionUtil()
        permissionUtil.askNotificationPermission()

        val startDestination = calculateStartDestination()

        setContent {
            AppRoot(startDestination)
        }
    }

    private fun calculateStartDestination(): Route {
        val bookingId = intent.getIntExtra(EXTRA_BOOKING_ID, -1).takeIf { it >= 0 }
        // Lending, SpaceBooking
        val bookingType = intent.getStringExtra(EXTRA_BOOKING_TYPE)

        when (intent.action) {
            ACTION_BOOKING_CONFIRMED -> {
                if (bookingId != null && bookingType != null) {
                    return Reservation(
                        lendingId = bookingId.takeIf { bookingType == "Lending" },
                        spaceBookingId = bookingId.takeIf { bookingType == "SpaceBooking" }
                    )
                }
            }
            ACTION_BOOKING_CANCELLED -> {
                // no destination for cancellations
            }
        }
        return Loading
    }

    companion object {
        const val EXTRA_BOOKING_ID = "booking_id"
        const val EXTRA_BOOKING_TYPE = "booking_type"

        const val ACTION_BOOKING_CONFIRMED = "org.centrexcursionistalcoi.app.booking_confirmed"
        const val ACTION_BOOKING_CANCELLED = "org.centrexcursionistalcoi.app.booking_cancelled"
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    AppRoot()
}
