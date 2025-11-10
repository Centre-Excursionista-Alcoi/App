package org.centrexcursionistalcoi.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.mmk.kmpnotifier.extensions.onCreateOrOnNewIntent
import com.mmk.kmpnotifier.notification.NotifierManager
import tech.kotlinlang.permission.PermissionInitiation

class MainActivity : NfcIntentHandlerActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        instance = this

        PermissionInitiation.setActivity(this)

        NotifierManager.onCreateOrOnNewIntent(intent)

        setContent {
            MainApp()
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

    companion object {
        var instance: MainActivity? = null
        private set
    }
}
