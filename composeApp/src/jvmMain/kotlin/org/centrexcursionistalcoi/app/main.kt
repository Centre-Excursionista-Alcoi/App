package org.centrexcursionistalcoi.app

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.mmk.kmpnotifier.notification.NotifierManager
import com.mmk.kmpnotifier.notification.configuration.NotificationPlatformConfiguration
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import kotlinx.coroutines.runBlocking
import org.centrexcursionistalcoi.app.push.PushNotifierListener
import org.centrexcursionistalcoi.app.storage.DriverFactory
import org.centrexcursionistalcoi.app.storage.createDatabase
import org.centrexcursionistalcoi.app.storage.databaseInstance

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    // Initialize the logging library
    Napier.base(DebugAntilog())

    initializeSentry()

    databaseInstance = runBlocking { createDatabase(DriverFactory()) }

    NotifierManager.initialize(
        NotificationPlatformConfiguration.Desktop(
            showPushNotification = false,
        )
    )

    NotifierManager.setLogger { message ->
        Napier.d(message, tag = "NotifierManager")
    }

    NotifierManager.addListener(PushNotifierListener)

    application {
        Window(
            title = "Centre Excursionista d'Alcoi",
            onCloseRequest = ::exitApplication
        ) {
            MainApp()
        }
    }
}
