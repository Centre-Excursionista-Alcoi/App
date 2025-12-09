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
        log.d { "Creating database..." }
        databaseInstance = createDatabase(DriverFactory())

        log.d { "Initializing push notifications..." }
        NotifierManager.initialize(
            NotificationPlatformConfiguration.Desktop(
                showPushNotification = false,
            )
        )

        log.d { "Setting logger for notifications..." }
        NotifierManager.setLogger { message ->
            log.d(tag = "NotifierManager") { message }
        }

        log.d { "Adding push notifier listener..." }
        NotifierManager.addListener(PushNotifierListener)
    }
}
