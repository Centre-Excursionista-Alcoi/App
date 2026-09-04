package org.centrexcursionistalcoi.app.database.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Update
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow
import org.centrexcursionistalcoi.app.database.entity.LendingEntity
import org.centrexcursionistalcoi.app.database.relation.LendingWithRelations
import kotlin.uuid.Uuid

@Dao
interface LendingDao {
    @Insert
    suspend fun insert(lending: LendingEntity)

    @Upsert
    suspend fun upsert(lending: LendingEntity)

    @Transaction
    @Query("SELECT * FROM Lendings WHERE id = :id")
    suspend fun get(id: Uuid): LendingWithRelations?

    @Transaction
    @Query("SELECT * FROM Lendings WHERE id IN (:ids)")
    suspend fun getByIdList(ids: List<Uuid>): List<LendingWithRelations>

    @Transaction
    @Query("SELECT * FROM Lendings WHERE id = :id")
    fun getAsFlow(id: Uuid): Flow<LendingWithRelations?>

    @Transaction
    @Query("SELECT * FROM Lendings ORDER BY timestamp DESC")
    suspend fun selectAll(): List<LendingWithRelations>

    @Transaction
    @Query("SELECT * FROM Lendings ORDER BY timestamp DESC")
    fun selectAllAsFlow(): Flow<List<LendingWithRelations>>

    @Query("DELETE FROM Lendings WHERE id = :id")
    suspend fun deleteById(id: Uuid)

    @Update
    suspend fun update(lending: org.centrexcursionistalcoi.app.database.entity.LendingEntity)
}
