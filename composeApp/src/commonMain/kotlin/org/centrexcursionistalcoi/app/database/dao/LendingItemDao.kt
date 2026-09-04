package org.centrexcursionistalcoi.app.database.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow
import kotlin.uuid.Uuid

@Dao
interface LendingItemDao {
    @Insert
    suspend fun insert(lendingItem: org.centrexcursionistalcoi.app.database.entity.LendingItemEntity)

    @Query("SELECT * FROM LendingItems WHERE lendingId = :lendingId AND itemId = :itemId")
    suspend fun get(lendingId: Uuid, itemId: Uuid): org.centrexcursionistalcoi.app.database.entity.LendingItemEntity?

    @Query("SELECT * FROM LendingItems WHERE lendingId = :lendingId")
    suspend fun getByLendingId(lendingId: Uuid): List<org.centrexcursionistalcoi.app.database.entity.LendingItemEntity>

    @Query("SELECT * FROM LendingItems WHERE lendingId = :lendingId")
    fun getByLendingIdAsFlow(lendingId: Uuid): Flow<List<org.centrexcursionistalcoi.app.database.entity.LendingItemEntity>>

    @Query("SELECT * FROM LendingItems")
    suspend fun selectAll(): List<org.centrexcursionistalcoi.app.database.entity.LendingItemEntity>

    @Query("SELECT * FROM LendingItems")
    fun selectAllAsFlow(): Flow<List<org.centrexcursionistalcoi.app.database.entity.LendingItemEntity>>

    @Query("DELETE FROM LendingItems WHERE lendingId = :lendingId AND itemId = :itemId")
    suspend fun deleteById(lendingId: Uuid, itemId: Uuid)

    @Query("DELETE FROM LendingItems WHERE lendingId = :lendingId")
    suspend fun deleteByLendingId(lendingId: Uuid)

    @Update
    suspend fun update(lendingItem: org.centrexcursionistalcoi.app.database.entity.LendingItemEntity)
}
