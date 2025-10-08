package org.centrexcursionistalcoi.app.database

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.map
import org.centrexcursionistalcoi.app.data.InventoryItem
import org.centrexcursionistalcoi.app.database.data.InventoryItems
import org.centrexcursionistalcoi.app.storage.databaseInstance

expect val InventoryItemsRepository : Repository<InventoryItem, Uuid>

object InventoryItemsSettingsRepository : SettingsRepository<InventoryItem, Uuid>("inventory_items", InventoryItem.serializer())

object InventoryItemsDatabaseRepository : DatabaseRepository<InventoryItem, Uuid>() {
    override val queries by lazy { databaseInstance.inventoryItemsQueries }

    override fun selectAllAsFlow(dispatcher: CoroutineDispatcher) = queries
        .selectAll()
        .asFlow()
        .mapToList(dispatcher)
        // Convert from DbDepartment to InventoryItem
        .map { items ->
            items.map { it.toInventoryItem() }
        }

    override suspend fun selectAll() = queries.selectAll().awaitAsList().map { it.toInventoryItem() }

    override suspend fun get(id: Uuid): InventoryItem? {
        return queries.get(id).awaitAsList().firstOrNull()?.toInventoryItem()
    }

    override suspend fun insert(item: InventoryItem) = queries.insert(
        id = item.id,
        variation = item.variation,
        type = item.type,
    )

    override suspend fun update(item: InventoryItem) = queries.update(
        id = item.id,
        variation = item.variation,
        type = item.type,
    )

    override suspend fun delete(id: Uuid) {
        queries.deleteById(id)
    }

    fun InventoryItems.toInventoryItem() = InventoryItem(
        id = id,
        variation = variation,
        type = type,
    )
}
