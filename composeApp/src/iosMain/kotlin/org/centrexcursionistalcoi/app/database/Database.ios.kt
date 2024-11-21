package org.centrexcursionistalcoi.app.database

import androidx.sqlite.SQLiteDriver
import androidx.sqlite.driver.NativeSQLiteDriver

actual val driver: SQLiteDriver = NativeSQLiteDriver()
