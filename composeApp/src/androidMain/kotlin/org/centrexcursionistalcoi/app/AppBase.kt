package org.centrexcursionistalcoi.app

import android.app.Application
import com.diamondedge.logging.logging
import com.mmk.kmpnotifier.notification.NotifierManager
import com.mmk.kmpnotifier.notification.configuration.NotificationPlatformConfiguration
import kotlinx.coroutines.runBlocking
import org.centrexcursionistalcoi.app.log.initializeSentry
import org.centrexcursionistalcoi.app.push.PushNotifierListener
import org.centrexcursionistalcoi.app.storage.DriverFactory
import org.centrexcursionistalcoi.app.storage.createDatabase
import org.centrexcursionistalcoi.app.storage.databaseInstance
import org.centrexcursionistalcoi.app.sync.BackgroundJobCoordinator

class AppBase : Application() {
    companion object {
        private val log = logging()
        
        var instance: AppBase? = null
            private set
    }

    override fun onCreate() {
        super.onCreate()

        instance = this

        initializeSentry()

        databaseInstance = runBlocking { createDatabase(DriverFactory(this@AppBase)) }

        BackgroundJobCoordinator.initialize(applicationContext)

        NotifierManager.initialize(
            configuration = NotificationPlatformConfiguration.Android(
                notificationIconResId = R.drawable.ic_notification,
                showPushNotification = false,
            )
        )

        NotifierManager.setLogger { message ->
            log.d(tag = "NotifierManager") { message }
        }

        NotifierManager.addListener(PushNotifierListener)
    }

    override fun onTerminate() {
        super.onTerminate()
        instance = null
    }
}
