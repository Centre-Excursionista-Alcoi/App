package org.centrexcursionistalcoi.app.database

import androidx.sqlite.SQLiteDriver
import androidx.sqlite.driver.AndroidSQLiteDriver

actual val driver: SQLiteDriver = AndroidSQLiteDriver()
