package org.centrexcursionistalcoi.app.database.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow
import kotlin.uuid.Uuid

@Dao
interface InventoryItemDao {
    @Insert
    suspend fun insert(inventoryItem: org.centrexcursionistalcoi.app.database.entity.InventoryItemEntity)

    @Transaction
    @Query("SELECT * FROM InventoryItems WHERE id = :id")
    suspend fun get(id: Uuid): org.centrexcursionistalcoi.app.database.relation.InventoryItemWithRelations?

    @Transaction
    @Query("SELECT * FROM InventoryItems WHERE id = :id")
    fun getAsFlow(id: Uuid): Flow<org.centrexcursionistalcoi.app.database.relation.InventoryItemWithRelations?>

    @Transaction
    @Query("SELECT * FROM InventoryItems ORDER BY id")
    suspend fun selectAll(): List<org.centrexcursionistalcoi.app.database.relation.InventoryItemWithRelations>

    @Transaction
    @Query("SELECT * FROM InventoryItems ORDER BY id")
    fun selectAllAsFlow(): Flow<List<org.centrexcursionistalcoi.app.database.relation.InventoryItemWithRelations>>

    /** Raw (relation-free) projection, used only where the type's full data isn't needed (e.g. cleanup by id). */
    @Query("SELECT * FROM InventoryItems WHERE type = :type ORDER BY id")
    suspend fun selectAllByType(type: Uuid): List<org.centrexcursionistalcoi.app.database.entity.InventoryItemEntity>

    @Transaction
    @Query("SELECT * FROM InventoryItems WHERE type = :type ORDER BY id")
    fun selectAllByTypeAsFlow(type: Uuid): Flow<List<org.centrexcursionistalcoi.app.database.relation.InventoryItemWithRelations>>

    @Query("DELETE FROM InventoryItems WHERE id = :id")
    suspend fun deleteById(id: Uuid)

    @Update
    suspend fun update(inventoryItem: org.centrexcursionistalcoi.app.database.entity.InventoryItemEntity)
}
