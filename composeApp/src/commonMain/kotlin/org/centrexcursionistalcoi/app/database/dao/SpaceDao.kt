package org.centrexcursionistalcoi.app.database.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow
import org.centrexcursionistalcoi.app.database.entity.SpaceEntity
import kotlin.uuid.Uuid

@Dao
interface SpaceDao {
    @Upsert
    suspend fun upsert(item: SpaceEntity)

    @Query("SELECT * FROM Spaces WHERE id = :id LIMIT 1")
    suspend fun get(id: Uuid): SpaceEntity?

    @Query("SELECT * FROM Spaces WHERE id IN (:ids)")
    suspend fun getByIdList(ids: List<Uuid>): List<SpaceEntity>

    @Query("SELECT * FROM Spaces WHERE id = :id LIMIT 1")
    fun getAsFlow(id: Uuid): Flow<SpaceEntity?>

    @Query("SELECT * FROM Spaces ORDER BY name")
    suspend fun selectAll(): List<SpaceEntity>

    @Query("SELECT * FROM Spaces ORDER BY name")
    fun selectAllAsFlow(): Flow<List<SpaceEntity>>

    @Query("DELETE FROM Spaces WHERE id = :id")
    suspend fun deleteById(id: Uuid)
}
