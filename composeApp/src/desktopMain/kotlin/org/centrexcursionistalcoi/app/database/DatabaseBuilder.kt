package org.centrexcursionistalcoi.app.database

import androidx.room.Room
import androidx.room.RoomDatabase
import kotlin.io.path.absolutePathString
import kotlin.io.path.div
import org.centrexcursionistalcoi.app.fs.dataDir

fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    val dbFile = dataDir() / "cea.db"
    return Room.databaseBuilder<AppDatabase>(
        name = dbFile.absolutePathString(),
    )
}
