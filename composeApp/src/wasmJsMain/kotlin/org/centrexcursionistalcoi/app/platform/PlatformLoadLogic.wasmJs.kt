package org.centrexcursionistalcoi.app.platform

import org.centrexcursionistalcoi.app.storage.DriverFactory
import org.centrexcursionistalcoi.app.storage.createDatabase
import org.centrexcursionistalcoi.app.storage.databaseInstance
import org.centrexcursionistalcoi.app.storage.isDatabaseReady

actual object PlatformLoadLogic {
    actual fun isReady(): Boolean {
        return isDatabaseReady
    }

    actual suspend fun load() {
        if (isReady()) return
        databaseInstance = createDatabase(DriverFactory())
    }
}
