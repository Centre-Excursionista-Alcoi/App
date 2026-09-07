package org.centrexcursionistalcoi.app.database.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow
import org.centrexcursionistalcoi.app.database.entity.ReceivedItemEntity
import kotlin.uuid.Uuid

@Dao
interface ReceivedItemDao {
    @Insert
    suspend fun insert(receivedItem: ReceivedItemEntity)

    @Query("SELECT * FROM ReceivedItems WHERE id = :id")
    suspend fun get(id: Uuid): ReceivedItemEntity?

    @Transaction
    @Query("SELECT * FROM ReceivedItems WHERE id IN (:ids)")
    suspend fun getByIdList(ids: List<Uuid>): List<ReceivedItemEntity>

    @Query("SELECT * FROM ReceivedItems WHERE id = :id")
    fun getAsFlow(id: Uuid): Flow<ReceivedItemEntity?>

    @Query("SELECT * FROM ReceivedItems ORDER BY receivedAt DESC")
    suspend fun selectAll(): List<ReceivedItemEntity>

    @Query("SELECT * FROM ReceivedItems ORDER BY receivedAt DESC")
    fun selectAllAsFlow(): Flow<List<ReceivedItemEntity>>

    @Query("DELETE FROM ReceivedItems WHERE lending = :lending")
    suspend fun deleteByLendingId(lending: Uuid)

    @Update
    suspend fun update(receivedItem: ReceivedItemEntity)
}
