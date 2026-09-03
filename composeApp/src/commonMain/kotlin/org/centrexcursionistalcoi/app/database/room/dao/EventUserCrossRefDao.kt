package org.centrexcursionistalcoi.app.database.room.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import org.centrexcursionistalcoi.app.database.room.entity.EventUserCrossRef
import kotlin.uuid.Uuid

@Dao
interface EventUserCrossRefDao {
    @Insert
    suspend fun insert(crossRef: EventUserCrossRef)

    @Query("DELETE FROM EventUsers WHERE eventId = :eventId")
    suspend fun deleteByEventId(eventId: Uuid)
}
