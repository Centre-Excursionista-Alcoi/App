package org.centrexcursionistalcoi.app.database.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow
import org.centrexcursionistalcoi.app.database.entity.SpaceLendingEntity
import kotlin.uuid.Uuid

@Dao
interface SpaceLendingDao {
    @Upsert
    suspend fun upsert(item: SpaceLendingEntity)

    @Query("SELECT * FROM SpaceLendings WHERE id = :id LIMIT 1")
    suspend fun get(id: Uuid): SpaceLendingEntity?

    @Query("SELECT * FROM SpaceLendings WHERE id IN (:ids)")
    suspend fun getByIdList(ids: List<Uuid>): List<SpaceLendingEntity>

    @Query("SELECT * FROM SpaceLendings WHERE id = :id LIMIT 1")
    fun getAsFlow(id: Uuid): Flow<SpaceLendingEntity?>

    @Query("SELECT * FROM SpaceLendings ORDER BY checkIn DESC")
    suspend fun selectAll(): List<SpaceLendingEntity>

    @Query("SELECT * FROM SpaceLendings ORDER BY checkIn DESC")
    fun selectAllAsFlow(): Flow<List<SpaceLendingEntity>>

    @Query("DELETE FROM SpaceLendings WHERE id = :id")
    suspend fun deleteById(id: Uuid)
}
