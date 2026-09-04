package org.centrexcursionistalcoi.app.database.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow
import kotlin.uuid.Uuid

@Dao
interface MemoryDao {
    @Insert
    suspend fun insert(memory: org.centrexcursionistalcoi.app.database.entity.MemoryEntity)

    @Transaction
    @Query("SELECT * FROM Memories WHERE id = :id")
    suspend fun get(id: Uuid): org.centrexcursionistalcoi.app.database.relation.MemoryWithRelations?

    @Transaction
    @Query("SELECT * FROM Memories WHERE id = :id")
    fun getAsFlow(id: Uuid): Flow<org.centrexcursionistalcoi.app.database.relation.MemoryWithRelations?>

    @Transaction
    @Query("SELECT * FROM Memories WHERE lending = :lending")
    suspend fun getByLendingId(lending: Uuid): List<org.centrexcursionistalcoi.app.database.relation.MemoryWithRelations>

    @Transaction
    @Query("SELECT * FROM Memories")
    suspend fun selectAll(): List<org.centrexcursionistalcoi.app.database.relation.MemoryWithRelations>

    @Transaction
    @Query("SELECT * FROM Memories")
    fun selectAllAsFlow(): Flow<List<org.centrexcursionistalcoi.app.database.relation.MemoryWithRelations>>

    @Query("DELETE FROM Memories WHERE id = :id")
    suspend fun deleteById(id: Uuid)

    @Update
    suspend fun update(memory: org.centrexcursionistalcoi.app.database.entity.MemoryEntity)
}
