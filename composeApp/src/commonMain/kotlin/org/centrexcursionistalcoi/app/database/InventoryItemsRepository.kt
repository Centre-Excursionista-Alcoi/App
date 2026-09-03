package org.centrexcursionistalcoi.app.database

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.centrexcursionistalcoi.app.data.ReferencedInventoryItem
import org.centrexcursionistalcoi.app.data.ReferencedInventoryItem.Companion.referenced
import org.centrexcursionistalcoi.app.database.room.entity.InventoryItemEntity.Companion.toEntity
import org.centrexcursionistalcoi.app.exception.MissingCrossReferenceException
import kotlin.uuid.Uuid

class InventoryItemsRepository(
    db: AppDatabase,
    private val inventoryItemTypesRepository: InventoryItemTypesRepository,
) : Repository<ReferencedInventoryItem, Uuid> {
    private val dao = db.inventoryItemDao()

    override fun selectAllAsFlow(): Flow<List<ReferencedInventoryItem>> {
        val typesFlow = inventoryItemTypesRepository.selectAllAsFlow()
        val itemsFlow = dao.selectAllAsFlow()
        return combine(typesFlow, itemsFlow) { types, items ->
            items.mapNotNull { item ->
                val type = types.find { it.id == item.type } ?: return@mapNotNull null
                item.toInventoryItem().referenced(type)
            }
        }
    }

    override suspend fun selectAll(): List<ReferencedInventoryItem> {
        val types = inventoryItemTypesRepository.selectAll()
        return dao.selectAll().map { item ->
            val type = types.firstOrNull { it.id == item.type } ?: throw MissingCrossReferenceException("InventoryItemType", item.type)
            item.toInventoryItem().referenced(type)
        }
    }

    fun selectAllWithTypeIdFlow(typeId: Uuid): Flow<List<ReferencedInventoryItem>> {
        val typesFlow = inventoryItemTypesRepository.selectAllAsFlow()
        val itemsFlow = dao.selectAllByTypeAsFlow(typeId)
        return combine(typesFlow, itemsFlow) { types, items ->
            items.mapNotNull { item ->
                val type = types.find { it.id == item.type } ?: return@mapNotNull null
                item.toInventoryItem().referenced(type)
            }
        }
    }

    override suspend fun get(id: Uuid): ReferencedInventoryItem? {
        val item = dao.get(id) ?: return null
        val type = inventoryItemTypesRepository.get(item.type) ?: throw MissingCrossReferenceException("InventoryItemType", item.type)
        return item.toInventoryItem().referenced(type)
    }

    override fun getAsFlow(id: Uuid): Flow<ReferencedInventoryItem?> {
        val typesFlow = inventoryItemTypesRepository.selectAllAsFlow()
        val itemFlow = dao.getAsFlow(id)
        return combine(typesFlow, itemFlow) { types, item ->
            item ?: return@combine null
            val type = types.find { it.id == item.type } ?: return@combine null
            item.toInventoryItem().referenced(type)
        }
    }

    override suspend fun insert(item: ReferencedInventoryItem) = dao.insert(
        item.dereference().toEntity()
    )

    override suspend fun update(item: ReferencedInventoryItem) = dao.update(
        item.dereference().toEntity()
    )

    override suspend fun delete(id: Uuid) {
        dao.deleteById(id)
    }
}
