package org.centrexcursionistalcoi.app.database.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow
import org.centrexcursionistalcoi.app.database.entity.QualificationGrantEntity

@Dao
interface QualificationGrantDao {
    @Upsert
    suspend fun upsertAll(items: List<QualificationGrantEntity>)

    @Query("DELETE FROM QualificationGrants WHERE qualificationId NOT IN (:keep)")
    suspend fun deleteAllExcept(keep: List<Uuid>)

    @Query("DELETE FROM QualificationGrants")
    suspend fun deleteAll()

    @Query("SELECT * FROM QualificationGrants")
    suspend fun selectAll(): List<QualificationGrantEntity>

    @Query("SELECT * FROM QualificationGrants")
    fun selectAllAsFlow(): Flow<List<QualificationGrantEntity>>
}
