package org.centrexcursionistalcoi.app.database.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Update
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow
import org.centrexcursionistalcoi.app.database.entity.UserEntity

@Dao
interface UserDao {
    // Upsert, not a plain insert: concurrent syncs check-then-insert the same user (e.g. a missing reference being
    // recovered while a sync runs), and the loser failed with `UNIQUE constraint failed: Users.sub`.
    @Upsert
    suspend fun upsert(user: UserEntity)

    @Query("SELECT * FROM Users WHERE sub = :sub")
    suspend fun get(sub: String): UserEntity?

    @Transaction
    @Query("SELECT * FROM Users WHERE sub IN (:subs)")
    suspend fun getBySubList(subs: List<String>): List<UserEntity>

    @Query("SELECT * FROM Users WHERE sub = :sub")
    fun getAsFlow(sub: String): Flow<UserEntity?>

    @Query("SELECT * FROM Users")
    suspend fun selectAll(): List<UserEntity>

    @Query("SELECT * FROM Users")
    fun selectAllAsFlow(): Flow<List<UserEntity>>

    @Query("DELETE FROM Users WHERE sub = :sub")
    suspend fun deleteById(sub: String)

    @Update
    suspend fun update(user: UserEntity)
}
