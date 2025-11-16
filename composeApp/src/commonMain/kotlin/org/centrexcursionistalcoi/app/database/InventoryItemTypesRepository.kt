package org.centrexcursionistalcoi.app.database

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.centrexcursionistalcoi.app.data.InventoryItemType
import org.centrexcursionistalcoi.app.database.data.InventoryItemTypes
import org.centrexcursionistalcoi.app.defaultAsyncDispatcher
import org.centrexcursionistalcoi.app.storage.databaseInstance

object InventoryItemTypesRepository : DatabaseRepository<InventoryItemType, Uuid>() {
    override val queries by lazy { databaseInstance.inventoryItemTypesQueries }

    override suspend fun get(id: Uuid): InventoryItemType? {
        return queries.get(id).awaitAsList().firstOrNull()?.toInventoryItemType()
    }

    override fun getAsFlow(id: Uuid, dispatcher: CoroutineDispatcher): Flow<InventoryItemType?> {
        return queries
            .get(id)
            .asFlow()
            .mapToList(dispatcher)
            .map { it.firstOrNull()?.toInventoryItemType() }
    }

    override fun selectAllAsFlow(dispatcher: CoroutineDispatcher) = queries
        .selectAll()
        .asFlow()
        .mapToList(dispatcher)
        // Convert from DbDepartment to InventoryItemType
        .map { items ->
            items.map { it.toInventoryItemType() }
        }

    override suspend fun selectAll() = queries.selectAll().awaitAsList().map { it.toInventoryItemType() }

    fun categoriesAsFlow(dispatcher: CoroutineDispatcher = defaultAsyncDispatcher): Flow<Set<String>> = queries
        .categories()
        .asFlow()
        .mapToList(dispatcher)
        .map { it.flatten().toSet() }

    override suspend fun insert(item: InventoryItemType) = queries.insert(
        id = item.id,
        displayName = item.displayName,
        description = item.description,
        categories = item.categories,
        image = item.image
    )

    override suspend fun update(item: InventoryItemType) = queries.update(
        id = item.id,
        displayName = item.displayName,
        description = item.description,
        categories = item.categories,
        image = item.image
    )

    override suspend fun delete(id: Uuid) {
        queries.deleteById(id)
    }

    fun InventoryItemTypes.toInventoryItemType() = InventoryItemType(
        id = id,
        displayName = displayName,
        description = description,
        categories = categories,
        image = image
    )
}
