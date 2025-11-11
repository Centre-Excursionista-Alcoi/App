package org.centrexcursionistalcoi.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.mmk.kmpnotifier.extensions.onCreateOrOnNewIntent
import com.mmk.kmpnotifier.notification.NotifierManager
import io.github.aakira.napier.Napier
import org.centrexcursionistalcoi.app.push.PushNotification
import tech.kotlinlang.permission.PermissionInitiation

class MainActivity : NfcIntentHandlerActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        instance = this

        PermissionInitiation.setActivity(this)

        NotifierManager.onCreateOrOnNewIntent(intent)

        val pushNotification = getPushNotificationFromIntent()

        setContent {
            MainApp(pushNotification)
        }
    }

    override fun onResume() {
        super.onResume()
        instance = this
    }

    override fun onDestroy() {
        super.onDestroy()
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
            Napier.d { "Got intent with extras, but no valid push notification could be inferred." }
            return null
        }
    }

    companion object {
        var instance: MainActivity? = null
        private set
    }
}
