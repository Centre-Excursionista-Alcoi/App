package org.centrexcursionistalcoi.app

import android.app.Application
import com.mmk.kmpnotifier.notification.NotifierManager
import com.mmk.kmpnotifier.notification.configuration.NotificationPlatformConfiguration
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import kotlinx.coroutines.runBlocking
import org.centrexcursionistalcoi.app.push.PushNotifierListener
import org.centrexcursionistalcoi.app.storage.DriverFactory
import org.centrexcursionistalcoi.app.storage.createDatabase
import org.centrexcursionistalcoi.app.storage.databaseInstance

class AppBase: Application() {
    override fun onCreate() {
        super.onCreate()

        Napier.base(DebugAntilog())

        initializeSentry()

        databaseInstance = runBlocking { createDatabase(DriverFactory(this@AppBase)) }

        NotifierManager.initialize(
            configuration = NotificationPlatformConfiguration.Android(
                notificationIconResId = R.drawable.ic_launcher_monochrome,
                showPushNotification = false,
            )
        )

        NotifierManager.setLogger { message ->
            Napier.d(message, tag = "NotifierManager")
        }

        NotifierManager.addListener(PushNotifierListener)
    }
}
