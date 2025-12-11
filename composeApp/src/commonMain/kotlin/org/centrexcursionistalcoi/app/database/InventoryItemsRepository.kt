package org.centrexcursionistalcoi.app.database

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import org.centrexcursionistalcoi.app.data.InventoryItem
import org.centrexcursionistalcoi.app.data.ReferencedInventoryItem
import org.centrexcursionistalcoi.app.data.ReferencedInventoryItem.Companion.referenced
import org.centrexcursionistalcoi.app.database.data.InventoryItems
import org.centrexcursionistalcoi.app.defaultAsyncDispatcher
import org.centrexcursionistalcoi.app.storage.databaseInstance
import kotlin.uuid.Uuid

object InventoryItemsRepository : DatabaseRepository<ReferencedInventoryItem, Uuid>() {
    override val queries by lazy { databaseInstance.inventoryItemsQueries }

    override fun selectAllAsFlow(dispatcher: CoroutineDispatcher): Flow<List<ReferencedInventoryItem>> {
        val typesFlow = InventoryItemTypesRepository.selectAllAsFlow(dispatcher)
        val itemsFlow = queries.selectAll().asFlow().mapToList(dispatcher)
        return combine(typesFlow, itemsFlow) { types, items ->
            items.mapNotNull { item ->
                val type = types.find { it.id == item.type } ?: return@mapNotNull null
                item.toInventoryItem().referenced(type)
            }
        }
    }

    override suspend fun selectAll(): List<ReferencedInventoryItem> {
        val types = InventoryItemTypesRepository.selectAll()
        return queries.selectAll().awaitAsList().map { item ->
            val type = types.first { it.id == item.type }
            item.toInventoryItem().referenced(type)
        }
    }

    fun selectAllWithTypeIdFlow(typeId: Uuid, dispatcher: CoroutineDispatcher = defaultAsyncDispatcher): Flow<List<ReferencedInventoryItem>> {
        val typesFlow = InventoryItemTypesRepository.selectAllAsFlow(dispatcher)
        val itemsFlow = queries.selectAllByType(typeId).asFlow().mapToList(dispatcher)
        return combine(typesFlow, itemsFlow) { types, items ->
            items.mapNotNull { item ->
                val type = types.find { it.id == item.type } ?: return@mapNotNull null
                item.toInventoryItem().referenced(type)
            }
        }
    }

    override suspend fun get(id: Uuid): ReferencedInventoryItem? {
        val item = queries.get(id).awaitAsList().firstOrNull() ?: return null
        val type = InventoryItemTypesRepository.get(item.type) ?: throw NoSuchElementException("Inventory item type not found: ${item.type}")
        return item.toInventoryItem().referenced(type)
    }

    override fun getAsFlow(id: Uuid, dispatcher: CoroutineDispatcher): Flow<ReferencedInventoryItem?> {
        val typesFlow = InventoryItemTypesRepository.selectAllAsFlow(dispatcher)
        val itemFlow = queries.get(id).asFlow().mapToList(dispatcher).map { it.firstOrNull() }
        return combine(typesFlow, itemFlow) { types, items ->
            items ?: return@combine null
            val type = types.find { it.id == items.type } ?: return@combine null
            items.toInventoryItem().referenced(type)
        }
    }

    override suspend fun insert(item: ReferencedInventoryItem) = queries.insert(
        id = item.id,
        variation = item.variation,
        type = item.type.id,
        nfcId = item.nfcId,
    )

    override suspend fun update(item: ReferencedInventoryItem) = queries.update(
        id = item.id,
        variation = item.variation,
        type = item.type.id,
        nfcId = item.nfcId,
    )

    override suspend fun delete(id: Uuid) {
        queries.deleteById(id)
    }

    fun InventoryItems.toInventoryItem() = InventoryItem(
        id = id,
        variation = variation,
        type = type,
        nfcId = nfcId,
    )
}
