package org.centrexcursionistalcoi.app.android

import android.app.Application
import com.diamondedge.logging.FixedLogLevel
import com.diamondedge.logging.KmLogging
import com.diamondedge.logging.PlatformLogger
import com.diamondedge.logging.logging
import com.mmk.kmpnotifier.notification.NotifierManager
import com.mmk.kmpnotifier.notification.configuration.NotificationPlatformConfiguration
import org.centrexcursionistalcoi.app.di.initKoin
import org.centrexcursionistalcoi.app.log.initializeSentry
import org.centrexcursionistalcoi.app.push.PushNotifierListener
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.annotation.KoinApplication
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

@KoinApplication
class AppBase : Application(), KoinComponent {
    companion object {
        private val log = logging()
        
        var instance: AppBase? = null
            private set
    }

    override fun onCreate() {
        super.onCreate()
        KmLogging.addLogger(PlatformLogger(FixedLogLevel(true)))

        instance = this

        initializeSentry()

        initKoin {
            androidContext(this@AppBase)
            workManagerFactory()
        }

        NotifierManager.initialize(
            configuration = NotificationPlatformConfiguration.Android(
                notificationIconResId = R.drawable.ic_notification,
                showPushNotification = false,
            )
        )

        NotifierManager.setLogger { message ->
            log.d(tag = "NotifierManager") { message }
        }

        NotifierManager.addListener(get<PushNotifierListener>())
    }

    override fun onTerminate() {
        super.onTerminate()
        instance = null
    }
}
