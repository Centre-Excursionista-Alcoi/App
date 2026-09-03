package org.centrexcursionistalcoi.app.database.room.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow
import org.centrexcursionistalcoi.app.database.room.entity.MemoryEntity
import kotlin.uuid.Uuid

@Dao
interface MemoryDao {
    @Insert
    suspend fun insert(memory: MemoryEntity)

    @Query("SELECT * FROM Memories WHERE id = :id")
    suspend fun get(id: Uuid): MemoryEntity?

    @Query("SELECT * FROM Memories WHERE id = :id")
    fun getAsFlow(id: Uuid): Flow<MemoryEntity?>

    @Query("SELECT * FROM Memories WHERE lending = :lending")
    suspend fun getByLendingId(lending: Uuid): List<MemoryEntity>

    @Query("SELECT * FROM Memories WHERE lending = :lending")
    fun getByLendingIdAsFlow(lending: Uuid): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM Memories")
    suspend fun selectAll(): List<MemoryEntity>

    @Query("SELECT * FROM Memories")
    fun selectAllAsFlow(): Flow<List<MemoryEntity>>

    @Query("DELETE FROM Memories WHERE id = :id")
    suspend fun deleteById(id: Uuid)

    @Update
    suspend fun update(memory: MemoryEntity)
}
