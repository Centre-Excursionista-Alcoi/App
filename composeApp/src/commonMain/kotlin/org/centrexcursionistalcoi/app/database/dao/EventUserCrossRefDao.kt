package org.centrexcursionistalcoi.app.database.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import kotlin.uuid.Uuid

@Dao
interface EventUserCrossRefDao {
    @Insert
    suspend fun insert(crossRef: org.centrexcursionistalcoi.app.database.entity.EventUserCrossRef)

    @Query("DELETE FROM EventUsers WHERE eventId = :eventId")
    suspend fun deleteByEventId(eventId: Uuid)
}
