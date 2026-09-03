package org.centrexcursionistalcoi.app.database.room.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow
import org.centrexcursionistalcoi.app.database.room.entity.InventoryItemEntity
import kotlin.uuid.Uuid

@Dao
interface InventoryItemDao {
    @Insert
    suspend fun insert(inventoryItem: InventoryItemEntity)

    @Query("SELECT * FROM InventoryItems WHERE id = :id")
    suspend fun get(id: Uuid): InventoryItemEntity?

    @Query("SELECT * FROM InventoryItems WHERE id = :id")
    fun getAsFlow(id: Uuid): Flow<InventoryItemEntity?>

    @Query("SELECT * FROM InventoryItems ORDER BY id")
    suspend fun selectAll(): List<InventoryItemEntity>

    @Query("SELECT * FROM InventoryItems ORDER BY id")
    fun selectAllAsFlow(): Flow<List<InventoryItemEntity>>

    @Query("SELECT * FROM InventoryItems WHERE type = :type ORDER BY id")
    suspend fun selectAllByType(type: Uuid): List<InventoryItemEntity>

    @Query("SELECT * FROM InventoryItems WHERE type = :type ORDER BY id")
    fun selectAllByTypeAsFlow(type: Uuid): Flow<List<InventoryItemEntity>>

    @Query("DELETE FROM InventoryItems WHERE id = :id")
    suspend fun deleteById(id: Uuid)

    @Update
    suspend fun update(inventoryItem: InventoryItemEntity)
}
