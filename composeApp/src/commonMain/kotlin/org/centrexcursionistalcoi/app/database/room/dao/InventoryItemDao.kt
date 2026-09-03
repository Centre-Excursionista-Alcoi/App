package org.centrexcursionistalcoi.app.database.room.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow
import org.centrexcursionistalcoi.app.database.room.entity.InventoryItemEntity
import org.centrexcursionistalcoi.app.database.room.relation.InventoryItemWithRelations
import kotlin.uuid.Uuid

@Dao
interface InventoryItemDao {
    @Insert
    suspend fun insert(inventoryItem: InventoryItemEntity)

    @Transaction
    @Query("SELECT * FROM InventoryItems WHERE id = :id")
    suspend fun get(id: Uuid): InventoryItemWithRelations?

    @Transaction
    @Query("SELECT * FROM InventoryItems WHERE id = :id")
    fun getAsFlow(id: Uuid): Flow<InventoryItemWithRelations?>

    @Transaction
    @Query("SELECT * FROM InventoryItems ORDER BY id")
    suspend fun selectAll(): List<InventoryItemWithRelations>

    @Transaction
    @Query("SELECT * FROM InventoryItems ORDER BY id")
    fun selectAllAsFlow(): Flow<List<InventoryItemWithRelations>>

    /** Raw (relation-free) projection, used only where the type's full data isn't needed (e.g. cleanup by id). */
    @Query("SELECT * FROM InventoryItems WHERE type = :type ORDER BY id")
    suspend fun selectAllByType(type: Uuid): List<InventoryItemEntity>

    @Transaction
    @Query("SELECT * FROM InventoryItems WHERE type = :type ORDER BY id")
    fun selectAllByTypeAsFlow(type: Uuid): Flow<List<InventoryItemWithRelations>>

    @Query("DELETE FROM InventoryItems WHERE id = :id")
    suspend fun deleteById(id: Uuid)

    @Update
    suspend fun update(inventoryItem: InventoryItemEntity)
}
