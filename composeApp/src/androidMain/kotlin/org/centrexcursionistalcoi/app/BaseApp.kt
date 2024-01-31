package org.centrexcursionistalcoi.app

import android.app.Application
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier

class BaseApp: Application() {
    override fun onCreate() {
        super.onCreate()

        Napier.base(DebugAntilog())
    }
}
