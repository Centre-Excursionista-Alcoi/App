package org.centrexcursionistalcoi.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.mmk.kmpnotifier.permission.permissionUtil
import io.github.aakira.napier.Napier
import io.github.vinceglb.filekit.core.FileKit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.centrexcursionistalcoi.app.auth.AccountManager
import org.centrexcursionistalcoi.app.error.ServerException
import org.centrexcursionistalcoi.app.network.AuthBackend
import org.centrexcursionistalcoi.app.push.PushNotifications
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

    override fun onStart() {
        super.onStart()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val account = AccountManager.get()
                if (account != null) {
                    // Login again to refresh the token
                    AuthBackend.login(account.first.email, account.second)
                }
                PushNotifications.refreshTokenOnServer()
            } catch (e: ServerException) {
                Napier.e(e) { "Could not refresh token." }
            }
        }
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
