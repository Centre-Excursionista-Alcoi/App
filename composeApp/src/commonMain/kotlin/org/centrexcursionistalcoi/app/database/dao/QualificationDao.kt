package org.centrexcursionistalcoi.app.database.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow
import org.centrexcursionistalcoi.app.database.entity.QualificationEntity

@Dao
interface QualificationDao {
    @Upsert
    suspend fun upsertAll(items: List<QualificationEntity>)

    @Query("DELETE FROM Qualifications WHERE id NOT IN (:keep)")
    suspend fun deleteAllExcept(keep: List<Uuid>)

    @Query("DELETE FROM Qualifications")
    suspend fun deleteAll()

    @Query("SELECT * FROM Qualifications")
    suspend fun selectAll(): List<QualificationEntity>

    @Query("SELECT * FROM Qualifications")
    fun selectAllAsFlow(): Flow<List<QualificationEntity>>
}
