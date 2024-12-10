package org.centrexcursionistalcoi.app

import android.app.Application
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import org.centrexcursionistalcoi.app.auth.AccountManager
import org.centrexcursionistalcoi.app.database.getDatabaseBuilder
import org.centrexcursionistalcoi.app.database.roomDatabaseBuilder
import org.centrexcursionistalcoi.app.push.AndroidPushNotifications
import org.centrexcursionistalcoi.app.settings.createDataStore
import org.centrexcursionistalcoi.app.settings.dataStore

class BaseApp : Application() {
    override fun onCreate() {
        super.onCreate()

        Napier.base(DebugAntilog())

        dataStore = createDataStore(this)
        roomDatabaseBuilder = getDatabaseBuilder(this)

        AndroidPushNotifications.initialize(this)

        AccountManager.initialize(this)
    }

    override fun onTerminate() {
        super.onTerminate()

        AccountManager.close()
    }
}
