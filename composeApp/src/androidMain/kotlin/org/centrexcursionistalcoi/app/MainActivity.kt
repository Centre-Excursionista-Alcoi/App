package org.centrexcursionistalcoi.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.diamondedge.logging.logging
import com.mmk.kmpnotifier.extensions.onCreateOrOnNewIntent
import com.mmk.kmpnotifier.notification.NotifierManager
import io.ktor.http.Url
import org.centrexcursionistalcoi.app.platform.PlatformAppUpdates
import org.centrexcursionistalcoi.app.push.PushNotification
import tech.kotlinlang.permission.PermissionInitiation

class MainActivity : NfcIntentHandlerActivity() {
    private val appUpdateResultLauncher = PlatformAppUpdates.registerForActivityResult(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        instance = this

        PermissionInitiation.setActivity(this)

        PlatformAppUpdates.initialize(this, appUpdateResultLauncher)

        NotifierManager.onCreateOrOnNewIntent(intent)

        val pushNotification = getPushNotificationFromIntent()
        val url = getUrlFromIntent()

        setContent {
            MainApp(url, pushNotification)
        }
    }

    override fun onResume() {
        super.onResume()
        instance = this

        PlatformAppUpdates.checkForUpdates(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        PlatformAppUpdates.stop()
        instance = null
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        NotifierManager.onCreateOrOnNewIntent(intent)
    }

    private fun getPushNotificationFromIntent(): PushNotification? {
        val extras = intent.extras ?: return null
        // convert the extras to a Map<String, *>
        @Suppress("DEPRECATION") val data = extras.keySet().associateWith { extras.get(it) }
        try {
            // Get the PushNotification object
            return PushNotification.fromData(data)
        } catch (_: IllegalArgumentException) {
            // Ignore invalid push notification data
            log.d { "Got intent with extras, but no valid push notification could be inferred." }
            return null
        }
    }

    private fun getUrlFromIntent(): Url? {
        val uri = intent.data ?: return null
        if (uri.scheme != "cea" || uri.host == "server.cea.arnaumora.com") return null
        return Url(uri.toString())
    }

    companion object {
        private val log = logging()

        var instance: MainActivity? = null
            private set
    }
}
