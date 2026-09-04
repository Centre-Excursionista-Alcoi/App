package org.centrexcursionistalcoi.app.database.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow
import org.centrexcursionistalcoi.app.database.entity.MemoryEntity
import org.centrexcursionistalcoi.app.database.relation.MemoryWithRelations
import kotlin.uuid.Uuid

@Dao
interface MemoryDao {
    @Insert
    suspend fun insert(memory: MemoryEntity)

    @Transaction
    @Query("SELECT * FROM Memories WHERE id = :id")
    suspend fun get(id: Uuid): MemoryWithRelations?

    @Transaction
    @Query("SELECT * FROM Memories WHERE id IN (:ids)")
    suspend fun getByIdList(ids: List<Uuid>): List<MemoryWithRelations>

    @Transaction
    @Query("SELECT * FROM Memories WHERE id = :id")
    fun getAsFlow(id: Uuid): Flow<MemoryWithRelations?>

    @Transaction
    @Query("SELECT * FROM Memories WHERE lending = :lending")
    suspend fun getByLendingId(lending: Uuid): List<MemoryWithRelations>

    @Transaction
    @Query("SELECT * FROM Memories")
    suspend fun selectAll(): List<MemoryWithRelations>

    @Transaction
    @Query("SELECT * FROM Memories")
    fun selectAllAsFlow(): Flow<List<MemoryWithRelations>>

    @Query("DELETE FROM Memories WHERE id = :id")
    suspend fun deleteById(id: Uuid)

    @Update
    suspend fun update(memory: org.centrexcursionistalcoi.app.database.entity.MemoryEntity)
}
