package org.centrexcursionistalcoi.app.database.room.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import org.centrexcursionistalcoi.app.database.room.entity.MemoryMemberCrossRef
import kotlin.uuid.Uuid

@Dao
interface MemoryMemberCrossRefDao {
    @Insert
    suspend fun insert(crossRef: MemoryMemberCrossRef)

    @Query("DELETE FROM MemoryMembers WHERE memoryId = :memoryId")
    suspend fun deleteByMemoryId(memoryId: Uuid)
}
