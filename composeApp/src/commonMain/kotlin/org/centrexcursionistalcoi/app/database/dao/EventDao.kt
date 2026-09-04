package org.centrexcursionistalcoi.app.database.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow
import kotlin.uuid.Uuid

@Dao
interface EventDao {
    @Insert
    suspend fun insert(event: org.centrexcursionistalcoi.app.database.entity.EventEntity)

    @Transaction
    @Query("SELECT * FROM Events WHERE id = :id LIMIT 1")
    suspend fun get(id: Uuid): org.centrexcursionistalcoi.app.database.relation.EventWithRelations?

    @Transaction
    @Query("SELECT * FROM Events WHERE id = :id LIMIT 1")
    fun getAsFlow(id: Uuid): Flow<org.centrexcursionistalcoi.app.database.relation.EventWithRelations?>

    @Transaction
    @Query("SELECT * FROM Events ORDER BY id")
    suspend fun selectAll(): List<org.centrexcursionistalcoi.app.database.relation.EventWithRelations>

    @Transaction
    @Query("SELECT * FROM Events ORDER BY id")
    fun selectAllAsFlow(): Flow<List<org.centrexcursionistalcoi.app.database.relation.EventWithRelations>>

    @Query("DELETE FROM Events WHERE id = :id")
    suspend fun deleteById(id: Uuid)

    @Update
    suspend fun update(event: org.centrexcursionistalcoi.app.database.entity.EventEntity)
}
