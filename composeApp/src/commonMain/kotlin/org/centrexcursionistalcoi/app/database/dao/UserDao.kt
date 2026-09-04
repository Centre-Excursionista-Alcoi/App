package org.centrexcursionistalcoi.app.database.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert
    suspend fun insert(user: org.centrexcursionistalcoi.app.database.entity.UserEntity)

    @Query("SELECT * FROM Users WHERE sub = :sub")
    suspend fun get(sub: String): org.centrexcursionistalcoi.app.database.entity.UserEntity?

    @Query("SELECT * FROM Users WHERE sub = :sub")
    fun getAsFlow(sub: String): Flow<org.centrexcursionistalcoi.app.database.entity.UserEntity?>

    @Query("SELECT * FROM Users")
    suspend fun selectAll(): List<org.centrexcursionistalcoi.app.database.entity.UserEntity>

    @Query("SELECT * FROM Users")
    fun selectAllAsFlow(): Flow<List<org.centrexcursionistalcoi.app.database.entity.UserEntity>>

    @Query("DELETE FROM Users WHERE sub = :sub")
    suspend fun deleteById(sub: String)

    @Update
    suspend fun update(user: org.centrexcursionistalcoi.app.database.entity.UserEntity)
}
