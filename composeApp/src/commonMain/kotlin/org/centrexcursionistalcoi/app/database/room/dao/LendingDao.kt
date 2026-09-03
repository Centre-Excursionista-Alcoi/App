package org.centrexcursionistalcoi.app.database.room.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow
import org.centrexcursionistalcoi.app.database.room.entity.LendingEntity
import kotlin.uuid.Uuid

@Dao
interface LendingDao {
    @Insert
    suspend fun insert(lending: LendingEntity)

    @Query("SELECT * FROM Lendings WHERE id = :id")
    suspend fun get(id: Uuid): LendingEntity?

    @Query("SELECT * FROM Lendings WHERE id = :id")
    fun getAsFlow(id: Uuid): Flow<LendingEntity?>

    @Query("SELECT * FROM Lendings ORDER BY timestamp DESC")
    suspend fun selectAll(): List<LendingEntity>

    @Query("SELECT * FROM Lendings ORDER BY timestamp DESC")
    fun selectAllAsFlow(): Flow<List<LendingEntity>>

    @Query("DELETE FROM Lendings WHERE id = :id")
    suspend fun deleteById(id: Uuid)

    @Update
    suspend fun update(lending: LendingEntity)
}
