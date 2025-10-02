package org.centrexcursionistalcoi.app.platform

import org.centrexcursionistalcoi.app.storage.DriverFactory
import org.centrexcursionistalcoi.app.storage.createDatabase
import org.centrexcursionistalcoi.app.storage.databaseInstance

actual object PlatformLoadLogic {
    actual suspend fun load() {
        databaseInstance = createDatabase(DriverFactory())
    }
}
