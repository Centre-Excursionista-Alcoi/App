package org.centrexcursionistalcoi.app.database.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import kotlin.uuid.Uuid

@Dao
interface MemoryMemberCrossRefDao {
    @Insert
    suspend fun insert(crossRef: org.centrexcursionistalcoi.app.database.entity.MemoryMemberCrossRef)

    @Query("DELETE FROM MemoryMembers WHERE memoryId = :memoryId")
    suspend fun deleteByMemoryId(memoryId: Uuid)
}
