package org.centrexcursionistalcoi.app

import android.app.Application
import com.mmk.kmpnotifier.notification.NotifierManager
import com.mmk.kmpnotifier.notification.configuration.NotificationPlatformConfiguration
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import org.centrexcursionistalcoi.app.auth.AccountManager

class BaseApp : Application() {
    override fun onCreate() {
        super.onCreate()

        Napier.base(DebugAntilog())

        /**
         * By default, showPushNotification value is true.
         * When set showPushNotification to false foreground push notification will not be shown to user.
         * You can still get notification content using #onPushNotification listener method.
         */
        NotifierManager.initialize(
            configuration = NotificationPlatformConfiguration.Android(
                notificationIconResId = R.drawable.ic_launcher_foreground,
                showPushNotification = false,
            )
        )

        AccountManager.initialize(this)
    }

    override fun onTerminate() {
        super.onTerminate()

        AccountManager.close()
    }
}
