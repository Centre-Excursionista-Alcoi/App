package org.centrexcursionistalcoi.app.database.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow
import kotlin.uuid.Uuid

@Dao
interface DepartmentDao {
    @Insert
    suspend fun insert(department: org.centrexcursionistalcoi.app.database.entity.DepartmentEntity)

    @Query("SELECT * FROM Departments WHERE id = :id LIMIT 1")
    suspend fun get(id: Uuid): org.centrexcursionistalcoi.app.database.entity.DepartmentEntity?

    @Query("SELECT * FROM Departments WHERE id = :id LIMIT 1")
    fun getAsFlow(id: Uuid): Flow<org.centrexcursionistalcoi.app.database.entity.DepartmentEntity?>

    @Query("SELECT * FROM Departments ORDER BY id")
    suspend fun selectAll(): List<org.centrexcursionistalcoi.app.database.entity.DepartmentEntity>

    @Query("SELECT * FROM Departments ORDER BY id")
    fun selectAllAsFlow(): Flow<List<org.centrexcursionistalcoi.app.database.entity.DepartmentEntity>>

    @Query("DELETE FROM Departments WHERE id = :id")
    suspend fun deleteById(id: Uuid)

    @Update
    suspend fun update(department: org.centrexcursionistalcoi.app.database.entity.DepartmentEntity)
}
