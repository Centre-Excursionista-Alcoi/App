package org.centrexcursionistalcoi.app.database.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow
import kotlin.uuid.Uuid

@Dao
interface PostDao {
    @Insert
    suspend fun insert(post: org.centrexcursionistalcoi.app.database.entity.PostEntity)

    @Transaction
    @Query("SELECT * FROM Posts WHERE id = :id")
    suspend fun get(id: Uuid): org.centrexcursionistalcoi.app.database.relation.PostWithRelations?

    @Transaction
    @Query("SELECT * FROM Posts WHERE id = :id")
    fun getAsFlow(id: Uuid): Flow<org.centrexcursionistalcoi.app.database.relation.PostWithRelations?>

    @Transaction
    @Query("SELECT * FROM Posts ORDER BY date DESC")
    suspend fun selectAll(): List<org.centrexcursionistalcoi.app.database.relation.PostWithRelations>

    @Transaction
    @Query("SELECT * FROM Posts ORDER BY date DESC")
    fun selectAllAsFlow(): Flow<List<org.centrexcursionistalcoi.app.database.relation.PostWithRelations>>

    @Query("DELETE FROM Posts WHERE id = :id")
    suspend fun deleteById(id: Uuid)

    @Update
    suspend fun update(post: org.centrexcursionistalcoi.app.database.entity.PostEntity)
}
