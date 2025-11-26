package org.centrexcursionistalcoi.app.platform

import com.diamondedge.logging.logging
import com.mmk.kmpnotifier.notification.NotifierManager
import com.mmk.kmpnotifier.notification.configuration.NotificationPlatformConfiguration
import org.centrexcursionistalcoi.app.push.PushNotifierListener
import org.centrexcursionistalcoi.app.storage.DriverFactory
import org.centrexcursionistalcoi.app.storage.createDatabase
import org.centrexcursionistalcoi.app.storage.databaseInstance

actual object PlatformLoadLogic {
    private val log = logging()

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
            log.d(tag = "NotifierManager") { message }
        }

        NotifierManager.addListener(PushNotifierListener)
    }
}
