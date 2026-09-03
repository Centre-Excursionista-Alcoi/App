package org.centrexcursionistalcoi.app.platform

import com.diamondedge.logging.logging
import com.mmk.kmpnotifier.notification.NotifierManager
import com.mmk.kmpnotifier.notification.configuration.NotificationPlatformConfiguration
import org.centrexcursionistalcoi.app.push.PushNotifierListener

actual object PlatformLoadLogic {
    private val log = logging()

    actual fun isReady(): Boolean {
        // nothing to check on JVM
        return true
    }

    actual suspend fun load() {
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
