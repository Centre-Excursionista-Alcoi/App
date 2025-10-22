package org.centrexcursionistalcoi.app.database

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import org.centrexcursionistalcoi.app.data.Lending
import org.centrexcursionistalcoi.app.data.ReferencedLending
import org.centrexcursionistalcoi.app.data.ReferencedLending.Companion.referenced
import org.centrexcursionistalcoi.app.database.InventoryItemTypesDatabaseRepository.toInventoryItemType
import org.centrexcursionistalcoi.app.database.InventoryItemsDatabaseRepository.toInventoryItem
import org.centrexcursionistalcoi.app.database.data.InventoryItemTypes
import org.centrexcursionistalcoi.app.database.data.InventoryItems
import org.centrexcursionistalcoi.app.database.data.LendingItems
import org.centrexcursionistalcoi.app.database.data.Lendings
import org.centrexcursionistalcoi.app.storage.databaseInstance

expect val LendingsRepository : Repository<ReferencedLending, Uuid>

object LendingsSettingsRepository : SettingsRepository<ReferencedLending, Uuid>("lendings", ReferencedLending.serializer())

object LendingsDatabaseRepository : DatabaseRepository<ReferencedLending, Uuid>() {
    override val queries by lazy { databaseInstance.lendingsQueries }
    private val lendingItemsQueries by lazy { databaseInstance.lendingItemsQueries }
    private val inventoryItemsQueries by lazy { databaseInstance.inventoryItemsQueries }
    private val inventoryItemTypesQueries by lazy { databaseInstance.inventoryItemTypesQueries }

    override suspend fun get(id: Uuid): ReferencedLending? {
        val lending = queries.get(id).executeAsOneOrNull() ?: return null
        val items = lendingItemsQueries.getByLendingId(id).awaitAsList()
        val inventoryItems = inventoryItemsQueries.selectAll().awaitAsList()
        val inventoryItemTypes = inventoryItemTypesQueries.selectAll().awaitAsList()
        return lending.toLending(items, inventoryItems, inventoryItemTypes)
    }

    override fun getAsFlow(id: Uuid, dispatcher: CoroutineDispatcher): Flow<ReferencedLending?> {
        val lendingFlow = queries.get(id).asFlow().mapToList(dispatcher).map { it.firstOrNull() }
        val lendingItemsFlow = lendingItemsQueries.getByLendingId(id).asFlow().mapToList(dispatcher)
        val inventoryItemsFlow = inventoryItemsQueries.selectAll().asFlow().mapToList(dispatcher)
        val inventoryItemTypesFlow = inventoryItemTypesQueries.selectAll().asFlow().mapToList(dispatcher)
        return combine(lendingFlow, lendingItemsFlow, inventoryItemsFlow, inventoryItemTypesFlow) { lending, items, inventoryItems, inventoryItemTypes ->
            lending?.toLending(items, inventoryItems, inventoryItemTypes)
        }
    }

    override fun selectAllAsFlow(dispatcher: CoroutineDispatcher): Flow<List<ReferencedLending>> {
        val lendingsFlow = queries.selectAll().asFlow().mapToList(dispatcher)
        val lendingItemsFlow = lendingItemsQueries.selectAll().asFlow().mapToList(dispatcher)
        val inventoryItemsQueries = inventoryItemsQueries.selectAll().asFlow().mapToList(dispatcher)
        val inventoryItemTypesFlow = inventoryItemTypesQueries.selectAll().asFlow().mapToList(dispatcher)
        return combine(lendingsFlow, lendingItemsFlow, inventoryItemsQueries, inventoryItemTypesFlow) { lendings, items, inventoryItems, inventoryItemTypes ->
            lendings.map { lending ->
                val relatedItems = items.filter { it.lendingId == lending.id }
                lending.toLending(relatedItems, inventoryItems, inventoryItemTypes)
            }
        }
    }

    override suspend fun selectAll(): List<ReferencedLending> {
        val lendings = queries.selectAll().awaitAsList()
        val items = lendingItemsQueries.selectAll().awaitAsList()
        val inventoryItems = inventoryItemsQueries.selectAll().awaitAsList()
        val inventoryItemTypes = inventoryItemTypesQueries.selectAll().awaitAsList()
        return lendings.map { lending ->
            val relatedItems = items.filter { it.lendingId == lending.id }
            lending.toLending(relatedItems, inventoryItems, inventoryItemTypes)
        }
    }

    override suspend fun insert(item: ReferencedLending): Long {
        queries.insert(
            id = item.id,
            userSub = item.userSub,
            timestamp = item.timestamp,
            fromDate = item.from,
            toDate = item.to,
            notes = item.notes,
            confirmed = item.confirmed,
            taken = item.taken,
            givenBy = item.givenBy,
            givenAt = item.givenAt,
            returned = item.returned,
            receivedBy = item.receivedBy,
            receivedAt = item.receivedAt,
            memorySubmitted = item.memorySubmitted,
            memorySubmittedAt = item.memorySubmittedAt,
            memoryDocumentId = item.memoryDocument,
            memoryReviewed = item.memoryReviewed,
        )
        for (inventoryItem in item.items) {
            lendingItemsQueries.insert(
                lendingId = item.id,
                itemId = inventoryItem.id
            )
        }
        return 1L
    }

    override suspend fun update(item: ReferencedLending): Long {
        queries.update(
            id = item.id,
            userSub = item.userSub,
            timestamp = item.timestamp,
            fromDate = item.from,
            toDate = item.to,
            notes = item.notes,
            confirmed = item.confirmed,
            taken = item.taken,
            givenBy = item.givenBy,
            givenAt = item.givenAt,
            returned = item.returned,
            receivedBy = item.receivedBy,
            receivedAt = item.receivedAt,
            memorySubmitted = item.memorySubmitted,
            memorySubmittedAt = item.memorySubmittedAt,
            memoryDocumentId = item.memoryDocument,
            memoryReviewed = item.memoryReviewed,
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

    fun Lendings.toLending(items: List<LendingItems>, inventoryItems: List<InventoryItems>, inventoryItemTypes: List<InventoryItemTypes>) = Lending(
        id = id,
        userSub = userSub,
        timestamp = timestamp,
        from = fromDate,
        to = toDate,
        notes = notes,
        confirmed = confirmed,
        taken = taken,
        givenBy = givenBy,
        givenAt = givenAt,
        returned = returned,
        receivedBy = receivedBy,
        receivedAt = receivedAt,
        memorySubmitted = memorySubmitted,
        memorySubmittedAt = memorySubmittedAt,
        memoryDocument = memoryDocumentId,
        memoryReviewed = memoryReviewed,
        items = items.mapNotNull { item -> inventoryItems.find { it.id == item.itemId }?.toInventoryItem() }
    ).referenced(
        inventoryItemTypes.map { types -> types.toInventoryItemType() }
    )
}
