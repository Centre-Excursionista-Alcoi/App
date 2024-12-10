package org.centrexcursionistalcoi.app.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.centrexcursionistalcoi.app.database.entity.admin.User

@Dao
interface AdminDao {
    @Insert
    suspend fun insertUser(user: User)

    @Update
    suspend fun updateUser(user: User)

    @Delete
    suspend fun deleteUser(user: User)

    @Query("SELECT * FROM User")
    suspend fun getAllUsers(): List<User>

    @Query("SELECT * FROM User")
    fun getAllUsersAsFlow(): Flow<List<User>>

    @Query("SELECT * FROM User WHERE email = :email")
    suspend fun getUser(email: String): User?
}
