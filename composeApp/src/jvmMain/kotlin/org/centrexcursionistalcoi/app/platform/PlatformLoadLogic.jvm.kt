package org.centrexcursionistalcoi.app.platform

import com.mmk.kmpnotifier.notification.NotifierManager
import com.mmk.kmpnotifier.notification.configuration.NotificationPlatformConfiguration
import io.github.aakira.napier.Napier
import org.centrexcursionistalcoi.app.push.PushNotifierListener
import org.centrexcursionistalcoi.app.storage.DriverFactory
import org.centrexcursionistalcoi.app.storage.createDatabase
import org.centrexcursionistalcoi.app.storage.databaseInstance

actual object PlatformLoadLogic {
    actual fun isReady(): Boolean {
        // nothing to check on JVM
        return true
    }

    actual suspend fun load() {
        databaseInstance = createDatabase(DriverFactory())

        NotifierManager.initialize(
            NotificationPlatformConfiguration.Desktop(
                showPushNotification = false,
            )
        )

        NotifierManager.setLogger { message ->
            Napier.d(message, tag = "NotifierManager")
        }

        NotifierManager.addListener(PushNotifierListener)
    }
}
