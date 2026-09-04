package org.centrexcursionistalcoi.app.platform

import com.diamondedge.logging.logging
import com.mmk.kmpnotifier.notification.NotifierManager
import com.mmk.kmpnotifier.notification.configuration.NotificationPlatformConfiguration
import org.centrexcursionistalcoi.app.push.PushNotifierListener
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

actual object PlatformLoadLogic : KoinComponent {
    private val log = logging()

    private val listener: PushNotifierListener by inject()

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
        NotifierManager.addListener(listener)
    }
}
