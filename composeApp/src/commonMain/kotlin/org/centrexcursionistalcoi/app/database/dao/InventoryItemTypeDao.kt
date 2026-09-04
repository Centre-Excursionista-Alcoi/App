package org.centrexcursionistalcoi.app.database.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow
import kotlin.uuid.Uuid

@Dao
interface InventoryItemTypeDao {
    @Insert
    suspend fun insert(inventoryItemType: org.centrexcursionistalcoi.app.database.entity.InventoryItemTypeEntity)

    @Transaction
    @Query("SELECT * FROM InventoryItemTypes WHERE id = :id LIMIT 1")
    suspend fun get(id: Uuid): org.centrexcursionistalcoi.app.database.relation.InventoryItemTypeWithRelations?

    @Transaction
    @Query("SELECT * FROM InventoryItemTypes WHERE id = :id LIMIT 1")
    fun getAsFlow(id: Uuid): Flow<org.centrexcursionistalcoi.app.database.relation.InventoryItemTypeWithRelations?>

    @Transaction
    @Query("SELECT * FROM InventoryItemTypes ORDER BY displayName")
    suspend fun selectAll(): List<org.centrexcursionistalcoi.app.database.relation.InventoryItemTypeWithRelations>

    @Transaction
    @Query("SELECT * FROM InventoryItemTypes ORDER BY displayName")
    fun selectAllAsFlow(): Flow<List<org.centrexcursionistalcoi.app.database.relation.InventoryItemTypeWithRelations>>

    @Query("SELECT * FROM InventoryItemTypes WHERE categories IS NOT NULL")
    suspend fun selectAllWithCategories(): List<org.centrexcursionistalcoi.app.database.entity.InventoryItemTypeEntity>

    @Query("SELECT * FROM InventoryItemTypes WHERE categories IS NOT NULL")
    fun selectAllWithCategoriesAsFlow(): Flow<List<org.centrexcursionistalcoi.app.database.entity.InventoryItemTypeEntity>>

    @Query("DELETE FROM InventoryItemTypes WHERE id = :id")
    suspend fun deleteById(id: Uuid)

    @Query("SELECT * FROM InventoryItemTypes WHERE department = :department")
    suspend fun selectByDepartmentId(department: Uuid): List<org.centrexcursionistalcoi.app.database.entity.InventoryItemTypeEntity>

    @Update
    suspend fun update(inventoryItemType: org.centrexcursionistalcoi.app.database.entity.InventoryItemTypeEntity)
}
