package org.centrexcursionistalcoi.app.database.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow
import kotlin.uuid.Uuid

@Dao
interface LendingDao {
    @Insert
    suspend fun insert(lending: org.centrexcursionistalcoi.app.database.entity.LendingEntity)

    @Transaction
    @Query("SELECT * FROM Lendings WHERE id = :id")
    suspend fun get(id: Uuid): org.centrexcursionistalcoi.app.database.relation.LendingWithRelations?

    @Transaction
    @Query("SELECT * FROM Lendings WHERE id = :id")
    fun getAsFlow(id: Uuid): Flow<org.centrexcursionistalcoi.app.database.relation.LendingWithRelations?>

    @Transaction
    @Query("SELECT * FROM Lendings ORDER BY timestamp DESC")
    suspend fun selectAll(): List<org.centrexcursionistalcoi.app.database.relation.LendingWithRelations>

    @Transaction
    @Query("SELECT * FROM Lendings ORDER BY timestamp DESC")
    fun selectAllAsFlow(): Flow<List<org.centrexcursionistalcoi.app.database.relation.LendingWithRelations>>

    @Query("DELETE FROM Lendings WHERE id = :id")
    suspend fun deleteById(id: Uuid)

    @Update
    suspend fun update(lending: org.centrexcursionistalcoi.app.database.entity.LendingEntity)
}
