package org.centrexcursionistalcoi.app.database

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.centrexcursionistalcoi.app.data.Lending
import org.centrexcursionistalcoi.app.database.InventoryItemsDatabaseRepository.toInventoryItem
import org.centrexcursionistalcoi.app.database.data.InventoryItems
import org.centrexcursionistalcoi.app.database.data.LendingItems
import org.centrexcursionistalcoi.app.database.data.Lendings
import org.centrexcursionistalcoi.app.storage.databaseInstance

expect val LendingsRepository : Repository<Lending, Uuid>

object LendingsSettingsRepository : SettingsRepository<Lending, Uuid>("lendings", Lending.serializer())

object LendingsDatabaseRepository : DatabaseRepository<Lending, Uuid>() {
    override val queries by lazy { databaseInstance.lendingsQueries }
    private val lendingItemsQueries by lazy { databaseInstance.lendingItemsQueries }
    private val inventoryItemsQueries by lazy { databaseInstance.inventoryItemsQueries }

    override suspend fun get(id: Uuid): Lending? {
        val lending = queries.get(id).executeAsOneOrNull() ?: return null
        val items = lendingItemsQueries.getByLendingId(id).awaitAsList()
        val inventoryItems = inventoryItemsQueries.selectAll().awaitAsList()
        return lending.toLending(items, inventoryItems)
    }

    override fun selectAllAsFlow(dispatcher: CoroutineDispatcher): Flow<List<Lending>> {
        val lendingsFlow = queries.selectAll().asFlow().mapToList(dispatcher)
        val lendingItemsFlow = lendingItemsQueries.selectAll().asFlow().mapToList(dispatcher)
        val inventoryItemsQueries = inventoryItemsQueries.selectAll().asFlow().mapToList(dispatcher)
        return combine(lendingsFlow, lendingItemsFlow, inventoryItemsQueries) { lendings, items, inventoryItems ->
            lendings.map { lending ->
                val relatedItems = items.filter { it.lendingId == lending.id }
                lending.toLending(relatedItems, inventoryItems)
            }
        }
    }

    override suspend fun selectAll(): List<Lending> {
        val lendings = queries.selectAll().awaitAsList()
        val items = lendingItemsQueries.selectAll().awaitAsList()
        val inventoryItems = inventoryItemsQueries.selectAll().awaitAsList()
        return lendings.map { lending ->
            val relatedItems = items.filter { it.lendingId == lending.id }
            lending.toLending(relatedItems, inventoryItems)
        }
    }

    override suspend fun insert(item: Lending): Long {
        queries.insert(
            id = item.id,
            userSub = item.userSub,
            timestamp = item.timestamp,
            fromDate = item.from,
            toDate = item.to,
            notes = item.notes,
            confirmed = item.confirmed,
            taken = item.taken,
            returned = item.returned
        )
        for (inventoryItem in item.items) {
            lendingItemsQueries.insert(
                lendingId = item.id,
                itemId = inventoryItem.id
            )
        }
        return 1L
    }

    override suspend fun update(item: Lending): Long {
        queries.update(
            id = item.id,
            userSub = item.userSub,
            timestamp = item.timestamp,
            fromDate = item.from,
            toDate = item.to,
            notes = item.notes,
            confirmed = item.confirmed,
            taken = item.taken,
            returned = item.returned
        )
        lendingItemsQueries.deleteByLendingId(item.id)
        for (inventoryItem in item.items) {
            lendingItemsQueries.insert(
                lendingId = item.id,
                itemId = inventoryItem.id
            )
        }
        return 1L
    }

    override suspend fun delete(id: Uuid) {
        queries.deleteById(id)
    }

    fun Lendings.toLending(items: List<LendingItems>, inventoryItems: List<InventoryItems>) = Lending(
        id = id,
        userSub = userSub,
        timestamp = timestamp,
        from = fromDate,
        to = toDate,
        notes = notes,
        confirmed = confirmed,
        taken = taken,
        returned = returned,
        items = items.mapNotNull { item -> inventoryItems.find { it.id == item.itemId }?.toInventoryItem() }
    )
}
