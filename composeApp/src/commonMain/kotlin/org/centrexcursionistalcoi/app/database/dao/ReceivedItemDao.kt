package org.centrexcursionistalcoi.app.database.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow
import kotlin.uuid.Uuid

@Dao
interface ReceivedItemDao {
    @Insert
    suspend fun insert(receivedItem: org.centrexcursionistalcoi.app.database.entity.ReceivedItemEntity)

    @Query("SELECT * FROM ReceivedItems WHERE id = :id")
    suspend fun get(id: Uuid): org.centrexcursionistalcoi.app.database.entity.ReceivedItemEntity?

    @Query("SELECT * FROM ReceivedItems WHERE id = :id")
    fun getAsFlow(id: Uuid): Flow<org.centrexcursionistalcoi.app.database.entity.ReceivedItemEntity?>

    @Query("SELECT * FROM ReceivedItems ORDER BY receivedAt DESC")
    suspend fun selectAll(): List<org.centrexcursionistalcoi.app.database.entity.ReceivedItemEntity>

    @Query("SELECT * FROM ReceivedItems ORDER BY receivedAt DESC")
    fun selectAllAsFlow(): Flow<List<org.centrexcursionistalcoi.app.database.entity.ReceivedItemEntity>>

    @Query("DELETE FROM ReceivedItems WHERE lending = :lending")
    suspend fun deleteByLendingId(lending: Uuid)

    @Update
    suspend fun update(receivedItem: org.centrexcursionistalcoi.app.database.entity.ReceivedItemEntity)
}
