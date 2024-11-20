package org.centrexcursionistalcoi.app

import android.app.Application
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import org.centrexcursionistalcoi.app.auth.AccountManager
import org.centrexcursionistalcoi.app.push.PushNotifications

class BaseApp : Application() {
    override fun onCreate() {
        super.onCreate()

        Napier.base(DebugAntilog())

        PushNotifications.initialize(this)

        AccountManager.initialize(this)
    }

    override fun onTerminate() {
        super.onTerminate()

        AccountManager.close()
    }
}
