package org.centrexcursionistalcoi.app.database.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow
import org.centrexcursionistalcoi.app.database.entity.SpaceKeyEntity
import kotlin.uuid.Uuid

@Dao
interface SpaceKeyDao {
    @Upsert
    suspend fun upsert(item: SpaceKeyEntity)

    @Query("SELECT * FROM SpaceKeys WHERE id = :id LIMIT 1")
    suspend fun get(id: Uuid): SpaceKeyEntity?

    @Query("SELECT * FROM SpaceKeys WHERE id IN (:ids)")
    suspend fun getByIdList(ids: List<Uuid>): List<SpaceKeyEntity>

    @Query("SELECT * FROM SpaceKeys WHERE id = :id LIMIT 1")
    fun getAsFlow(id: Uuid): Flow<SpaceKeyEntity?>

    @Query("SELECT * FROM SpaceKeys ORDER BY label")
    suspend fun selectAll(): List<SpaceKeyEntity>

    @Query("SELECT * FROM SpaceKeys ORDER BY label")
    fun selectAllAsFlow(): Flow<List<SpaceKeyEntity>>

    @Query("SELECT * FROM SpaceKeys WHERE type = :type ORDER BY label")
    fun getByTypeAsFlow(type: Uuid): Flow<List<SpaceKeyEntity>>

    @Query("DELETE FROM SpaceKeys WHERE id = :id")
    suspend fun deleteById(id: Uuid)
}
