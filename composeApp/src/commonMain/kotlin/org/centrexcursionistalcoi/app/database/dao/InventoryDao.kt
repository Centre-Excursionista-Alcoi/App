package org.centrexcursionistalcoi.app.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.centrexcursionistalcoi.app.database.entity.Item
import org.centrexcursionistalcoi.app.database.entity.ItemType
import org.centrexcursionistalcoi.app.database.entity.Section
import org.centrexcursionistalcoi.app.database.relationship.ItemTypeWithItems
import org.centrexcursionistalcoi.app.database.relationship.SectionWithItemTypes

@Dao
interface InventoryDao {
    @Insert
    suspend fun insertSection(section: Section)

    @Update
    suspend fun updateSection(section: Section)

    @Delete
    suspend fun deleteSection(section: Section)

    @Query("SELECT * FROM Section")
    suspend fun getAllSections(): List<Section>

    @Query("SELECT * FROM Section")
    fun getAllSectionsAsFlow(): Flow<List<Section>>

    @Transaction
    @Query("SELECT * FROM Section")
    suspend fun getSectionsWithItemTypes(): List<SectionWithItemTypes>


    @Insert
    suspend fun insertItemType(itemType: ItemType)

    @Update
    suspend fun updateItemType(itemType: ItemType)

    @Delete
    suspend fun deleteItemType(itemType: ItemType)

    @Query("SELECT * FROM ItemType")
    suspend fun getAllItemTypes(): List<ItemType>

    @Query("SELECT * FROM ItemType")
    fun getAllItemTypesAsFlow(): Flow<List<ItemType>>

    @Transaction
    @Query("SELECT * FROM ItemType")
    suspend fun getItemTypesWithItems(): List<ItemTypeWithItems>


    @Insert
    suspend fun insertItem(item: Item)

    @Update
    suspend fun updateItem(item: Item)

    @Delete
    suspend fun deleteItem(item: Item)

    @Query("SELECT * FROM Item")
    suspend fun getAllItems(): List<Item>

    @Query("SELECT * FROM Item")
    fun getAllItemsAsFlow(): Flow<List<Item>>

    @Query("SELECT * FROM Item WHERE id IN (:filterIds)")
    suspend fun getAllItemsFromIds(filterIds: List<Int>): List<Item>
}
