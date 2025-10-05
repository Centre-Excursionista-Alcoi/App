package org.centrexcursionistalcoi.app

import android.app.Application
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import kotlinx.coroutines.runBlocking
import org.centrexcursionistalcoi.app.storage.DriverFactory
import org.centrexcursionistalcoi.app.storage.createDatabase
import org.centrexcursionistalcoi.app.storage.databaseInstance

class AppBase: Application() {
    override fun onCreate() {
        super.onCreate()

        Napier.base(DebugAntilog())

        databaseInstance = runBlocking { createDatabase(DriverFactory(this@AppBase)) }
    }
}
