package org.centrexcursionistalcoi.app.database.room.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow
import org.centrexcursionistalcoi.app.database.room.entity.EventEntity
import org.centrexcursionistalcoi.app.database.room.relation.EventWithRelations
import kotlin.uuid.Uuid

@Dao
interface EventDao {
    @Insert
    suspend fun insert(event: EventEntity)

    @Transaction
    @Query("SELECT * FROM Events WHERE id = :id LIMIT 1")
    suspend fun get(id: Uuid): EventWithRelations?

    @Transaction
    @Query("SELECT * FROM Events WHERE id = :id LIMIT 1")
    fun getAsFlow(id: Uuid): Flow<EventWithRelations?>

    @Transaction
    @Query("SELECT * FROM Events ORDER BY id")
    suspend fun selectAll(): List<EventWithRelations>

    @Transaction
    @Query("SELECT * FROM Events ORDER BY id")
    fun selectAllAsFlow(): Flow<List<EventWithRelations>>

    @Query("DELETE FROM Events WHERE id = :id")
    suspend fun deleteById(id: Uuid)

    @Update
    suspend fun update(event: EventEntity)
}
