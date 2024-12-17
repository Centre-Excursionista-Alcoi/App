package org.centrexcursionistalcoi.app

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import ceaapp.composeapp.generated.resources.*
import com.mmk.kmpnotifier.extensions.composeDesktopResourcesPath
import com.mmk.kmpnotifier.notification.NotifierManager
import com.mmk.kmpnotifier.notification.configuration.NotificationPlatformConfiguration
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import java.io.File
import org.centrexcursionistalcoi.app.database.getDatabaseBuilder
import org.centrexcursionistalcoi.app.database.roomDatabaseBuilder
import org.centrexcursionistalcoi.app.sentry.initializeSentry
import org.jetbrains.compose.resources.painterResource

fun main() {
    Napier.base(DebugAntilog())

    initializeSentry()

    roomDatabaseBuilder = getDatabaseBuilder()

    NotifierManager.initialize(
        NotificationPlatformConfiguration.Desktop(
            showPushNotification = false,
            notificationIconPath = composeDesktopResourcesPath() + File.separator + "ic_notification.png"
        )
    )

    application {
        Window(
            onCloseRequest = ::exitApplication,
            icon = painterResource(Res.drawable.CEA),
            title = "CEA App",
        ) {
            AppRoot()
        }
    }
}
