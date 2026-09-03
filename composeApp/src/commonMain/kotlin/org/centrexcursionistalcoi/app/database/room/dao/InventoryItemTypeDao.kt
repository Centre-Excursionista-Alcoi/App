package org.centrexcursionistalcoi.app.database.room.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow
import org.centrexcursionistalcoi.app.database.room.entity.InventoryItemTypeEntity
import kotlin.uuid.Uuid

@Dao
interface InventoryItemTypeDao {
    @Insert
    suspend fun insert(inventoryItemType: InventoryItemTypeEntity)

    @Query("SELECT * FROM InventoryItemTypes WHERE id = :id LIMIT 1")
    suspend fun get(id: Uuid): InventoryItemTypeEntity?

    @Query("SELECT * FROM InventoryItemTypes WHERE id = :id LIMIT 1")
    fun getAsFlow(id: Uuid): Flow<InventoryItemTypeEntity?>

    @Query("SELECT * FROM InventoryItemTypes ORDER BY displayName")
    suspend fun selectAll(): List<InventoryItemTypeEntity>

    @Query("SELECT * FROM InventoryItemTypes ORDER BY displayName")
    fun selectAllAsFlow(): Flow<List<InventoryItemTypeEntity>>

    @Query("SELECT * FROM InventoryItemTypes WHERE categories IS NOT NULL")
    suspend fun selectAllWithCategories(): List<InventoryItemTypeEntity>

    @Query("SELECT * FROM InventoryItemTypes WHERE categories IS NOT NULL")
    fun selectAllWithCategoriesAsFlow(): Flow<List<InventoryItemTypeEntity>>

    @Query("DELETE FROM InventoryItemTypes WHERE id = :id")
    suspend fun deleteById(id: Uuid)

    @Query("SELECT * FROM InventoryItemTypes WHERE department = :department")
    suspend fun selectByDepartmentId(department: Uuid): List<InventoryItemTypeEntity>

    @Update
    suspend fun update(inventoryItemType: InventoryItemTypeEntity)
}
