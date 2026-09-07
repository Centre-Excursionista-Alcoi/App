package org.centrexcursionistalcoi.app.database.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Update
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow
import org.centrexcursionistalcoi.app.database.entity.PostEntity
import org.centrexcursionistalcoi.app.database.relation.PostWithRelations
import kotlin.uuid.Uuid

@Dao
interface PostDao {
    @Insert
    suspend fun insert(post: PostEntity)

    @Upsert
    suspend fun upsert(post: PostEntity)

    @Transaction
    @Query("SELECT * FROM Posts WHERE id = :id")
    suspend fun get(id: Uuid): PostWithRelations?

    @Transaction
    @Query("SELECT * FROM Posts WHERE id IN (:ids)")
    suspend fun getByIdList(ids: List<Uuid>): List<PostWithRelations>

    @Transaction
    @Query("SELECT * FROM Posts WHERE id = :id")
    fun getAsFlow(id: Uuid): Flow<PostWithRelations?>

    @Transaction
    @Query("SELECT * FROM Posts ORDER BY date DESC")
    suspend fun selectAll(): List<PostWithRelations>

    @Transaction
    @Query("SELECT * FROM Posts ORDER BY date DESC")
    fun selectAllAsFlow(): Flow<List<PostWithRelations>>

    @Query("DELETE FROM Posts WHERE id = :id")
    suspend fun deleteById(id: Uuid)

    @Update
    suspend fun update(post: PostEntity)
}
