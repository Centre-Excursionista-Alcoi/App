package org.centrexcursionistalcoi.app

import android.app.Application
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import org.centrexcursionistalcoi.app.auth.AccountManager

class BaseApp : Application() {
    override fun onCreate() {
        super.onCreate()

        Napier.base(DebugAntilog())

        AccountManager.initialize(this)
    }
}
