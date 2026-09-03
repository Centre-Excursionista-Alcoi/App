package org.centrexcursionistalcoi.app.database.room.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow
import org.centrexcursionistalcoi.app.database.room.entity.PostEntity
import kotlin.uuid.Uuid

@Dao
interface PostDao {
    @Insert
    suspend fun insert(post: PostEntity)

    @Query("SELECT * FROM Posts WHERE id = :id")
    suspend fun get(id: Uuid): PostEntity?

    @Query("SELECT * FROM Posts WHERE id = :id")
    fun getAsFlow(id: Uuid): Flow<PostEntity?>

    @Query("SELECT * FROM Posts ORDER BY date DESC")
    suspend fun selectAll(): List<PostEntity>

    @Query("SELECT * FROM Posts ORDER BY date DESC")
    fun selectAllAsFlow(): Flow<List<PostEntity>>

    @Query("DELETE FROM Posts WHERE id = :id")
    suspend fun deleteById(id: Uuid)

    @Update
    suspend fun update(post: PostEntity)
}
