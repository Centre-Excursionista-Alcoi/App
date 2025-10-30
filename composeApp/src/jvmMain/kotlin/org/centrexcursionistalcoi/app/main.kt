package org.centrexcursionistalcoi.app

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import cea_app.composeapp.generated.resources.*
import com.mmk.kmpnotifier.notification.NotifierManager
import com.mmk.kmpnotifier.notification.configuration.NotificationPlatformConfiguration
import dev.datlag.kcef.KCEF
import io.github.aakira.napier.Antilog
import io.github.aakira.napier.LogLevel
import io.github.aakira.napier.Napier
import kotlinx.coroutines.runBlocking
import org.centrexcursionistalcoi.app.auth.AuthFlowWindow
import org.centrexcursionistalcoi.app.push.PushNotifierListener
import org.centrexcursionistalcoi.app.storage.DriverFactory
import org.centrexcursionistalcoi.app.storage.createDatabase
import org.centrexcursionistalcoi.app.storage.databaseInstance
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    // Initialize the logging library
    Napier.base(
        object : Antilog() {
            override fun performLog(priority: LogLevel, tag: String?, throwable: Throwable?, message: String?) {
                val out = if (priority == LogLevel.ERROR) System.err else System.out
                out.println("[$priority] ${tag.orEmpty()}: ${message.orEmpty()}")
                throwable?.printStackTrace()
            }
        }
    )

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
        AuthFlowWindow()

        Window(
            title = "Centre Excursionista d'Alcoi",
            icon = painterResource(Res.drawable.icon),
            state = rememberWindowState(
                size = DpSize(1000.dp, 800.dp),
            ),
            onCloseRequest = {
                KCEF.disposeBlocking()
                exitApplication()
            }
        ) {
            MainApp()
        }
    }
}
