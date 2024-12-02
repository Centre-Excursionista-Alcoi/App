package org.centrexcursionistalcoi.app.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.centrexcursionistalcoi.app.database.entity.Notification

@Dao
interface NotificationsDao {
    @Insert
    suspend fun insert(notification: Notification)

    @Update
    suspend fun update(notification: Notification)

    @Delete
    suspend fun delete(notification: Notification)

    @Query("SELECT * FROM Notification")
    suspend fun getAllNotifications(): List<Notification>

    @Query("SELECT * FROM Notification")
    fun getAllNotificationsAsFlow(): Flow<List<Notification>>
}
