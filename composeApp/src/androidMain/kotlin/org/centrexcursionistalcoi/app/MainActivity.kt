package org.centrexcursionistalcoi.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import tech.kotlinlang.permission.PermissionInitiation

class MainActivity : NfcIntentHandlerActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        instance = this

        PermissionInitiation.setActivity(this)

        setContent {
            MainApp()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    companion object {
        var instance: MainActivity? = null
        private set
    }
}
