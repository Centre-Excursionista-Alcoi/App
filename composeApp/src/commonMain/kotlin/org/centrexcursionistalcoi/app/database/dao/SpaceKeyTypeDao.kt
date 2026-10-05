package org.centrexcursionistalcoi.app.database.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow
import org.centrexcursionistalcoi.app.database.entity.SpaceKeyTypeEntity
import kotlin.uuid.Uuid

@Dao
interface SpaceKeyTypeDao {
    @Upsert
    suspend fun upsert(item: SpaceKeyTypeEntity)

    @Query("SELECT * FROM SpaceKeyTypes WHERE id = :id LIMIT 1")
    suspend fun get(id: Uuid): SpaceKeyTypeEntity?

    @Query("SELECT * FROM SpaceKeyTypes WHERE id IN (:ids)")
    suspend fun getByIdList(ids: List<Uuid>): List<SpaceKeyTypeEntity>

    @Query("SELECT * FROM SpaceKeyTypes WHERE id = :id LIMIT 1")
    fun getAsFlow(id: Uuid): Flow<SpaceKeyTypeEntity?>

    @Query("SELECT * FROM SpaceKeyTypes ORDER BY name")
    suspend fun selectAll(): List<SpaceKeyTypeEntity>

    @Query("SELECT * FROM SpaceKeyTypes ORDER BY name")
    fun selectAllAsFlow(): Flow<List<SpaceKeyTypeEntity>>

    @Query("DELETE FROM SpaceKeyTypes WHERE id = :id")
    suspend fun deleteById(id: Uuid)
}
