package org.centrexcursionistalcoi.app.database

import androidx.room3.Room
import androidx.room3.RoomDatabase
import java.io.File

fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    val dbFile = File(System.getProperty("java.io.tmpdir"), DATABASE_FILE_NAME)
    return Room.databaseBuilder<AppDatabase>(
        name = dbFile.absolutePath,
    )
}
