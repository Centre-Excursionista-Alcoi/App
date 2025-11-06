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
import org.centrexcursionistalcoi.app.data.ReceivedItem
import org.centrexcursionistalcoi.app.data.ReferencedLending
import org.centrexcursionistalcoi.app.data.ReferencedLending.Companion.referenced
import org.centrexcursionistalcoi.app.database.InventoryItemTypesDatabaseRepository.toInventoryItemType
import org.centrexcursionistalcoi.app.database.InventoryItemsDatabaseRepository.toInventoryItem
import org.centrexcursionistalcoi.app.database.UsersDatabaseRepository.toUser
import org.centrexcursionistalcoi.app.database.data.InventoryItemTypes
import org.centrexcursionistalcoi.app.database.data.InventoryItems
import org.centrexcursionistalcoi.app.database.data.LendingItems
import org.centrexcursionistalcoi.app.database.data.Lendings
import org.centrexcursionistalcoi.app.database.data.ReceivedItems
import org.centrexcursionistalcoi.app.database.data.Users
import org.centrexcursionistalcoi.app.storage.databaseInstance

expect val LendingsRepository : Repository<ReferencedLending, Uuid>

object LendingsSettingsRepository : SettingsRepository<ReferencedLending, Uuid>("lendings", ReferencedLending.serializer())

object LendingsDatabaseRepository : DatabaseRepository<ReferencedLending, Uuid>() {
    override val queries by lazy { databaseInstance.lendingsQueries }
    private val lendingItemsQueries by lazy { databaseInstance.lendingItemsQueries }
    private val inventoryItemsQueries by lazy { databaseInstance.inventoryItemsQueries }
    private val inventoryItemTypesQueries by lazy { databaseInstance.inventoryItemTypesQueries }
    private val usersQueries by lazy { databaseInstance.usersQueries }
    private val receivedItemsQueries by lazy { databaseInstance.receivedItemsQueries }

    override suspend fun get(id: Uuid): ReferencedLending? {
        val lending = queries.get(id).executeAsOneOrNull() ?: return null
        val items = lendingItemsQueries.getByLendingId(id).awaitAsList()
        val inventoryItems = inventoryItemsQueries.selectAll().awaitAsList()
        val inventoryItemTypes = inventoryItemTypesQueries.selectAll().awaitAsList()
        val users = usersQueries.selectAll().awaitAsList()
        val receivedItems = receivedItemsQueries.selectAll().awaitAsList()
        return lending.toLending(items, inventoryItems, inventoryItemTypes, users, receivedItems)
    }

    override fun getAsFlow(id: Uuid, dispatcher: CoroutineDispatcher): Flow<ReferencedLending?> {
        val lendingFlow = queries.get(id).asFlow().mapToList(dispatcher).map { it.firstOrNull() }
        val lendingItemsFlow = lendingItemsQueries.getByLendingId(id).asFlow().mapToList(dispatcher)
        val inventoryItemsFlow = inventoryItemsQueries.selectAll().asFlow().mapToList(dispatcher)
        val inventoryItemTypesFlow = inventoryItemTypesQueries.selectAll().asFlow().mapToList(dispatcher)
        val usersQueries = usersQueries.selectAll().asFlow().mapToList(dispatcher)
        val receivedItemsFlow = receivedItemsQueries.selectAll().asFlow().mapToList(dispatcher)
        @Suppress("UNCHECKED_CAST")
        return combine(
            lendingFlow,
            lendingItemsFlow,
            inventoryItemsFlow,
            inventoryItemTypesFlow,
            usersQueries,
            receivedItemsFlow,
        ) { flows ->
            val lending = flows[0] as Lendings?
            val items = flows[1] as List<LendingItems>
            val inventoryItems = flows[2] as List<InventoryItems>
            val inventoryItemTypes = flows[3] as List<InventoryItemTypes>
            val users = flows[4] as List<Users>
            val receivedItems = flows[5] as List<ReceivedItems>
            lending?.toLending(items, inventoryItems, inventoryItemTypes, users, receivedItems)
        }
    }

    override fun selectAllAsFlow(dispatcher: CoroutineDispatcher): Flow<List<ReferencedLending>> {
        val lendingsFlow = queries.selectAll().asFlow().mapToList(dispatcher)
        val lendingItemsFlow = lendingItemsQueries.selectAll().asFlow().mapToList(dispatcher)
        val inventoryItemsQueries = inventoryItemsQueries.selectAll().asFlow().mapToList(dispatcher)
        val inventoryItemTypesFlow = inventoryItemTypesQueries.selectAll().asFlow().mapToList(dispatcher)
        val usersQueries = usersQueries.selectAll().asFlow().mapToList(dispatcher)
        val receivedItemsFlow = receivedItemsQueries.selectAll().asFlow().mapToList(dispatcher)
        @Suppress("UNCHECKED_CAST")
        return combine(
            lendingsFlow,
            lendingItemsFlow,
            inventoryItemsQueries,
            inventoryItemTypesFlow,
            usersQueries,
            receivedItemsFlow,
        ) { flows ->
            val lendings = flows[0] as List<Lendings>
            val items = flows[1] as List<LendingItems>
            val inventoryItems = flows[2] as List<InventoryItems>
            val inventoryItemTypes = flows[3] as List<InventoryItemTypes>
            val users = flows[4] as List<Users>
            val receivedItems = flows[5] as List<ReceivedItems>
            lendings.map { lending ->
                val relatedItems = items.filter { it.lendingId == lending.id }
                lending.toLending(relatedItems, inventoryItems, inventoryItemTypes, users, receivedItems)
            }
        }
    }

    override suspend fun selectAll(): List<ReferencedLending> {
        val lendings = queries.selectAll().awaitAsList()
        val items = lendingItemsQueries.selectAll().awaitAsList()
        val inventoryItems = inventoryItemsQueries.selectAll().awaitAsList()
        val inventoryItemTypes = inventoryItemTypesQueries.selectAll().awaitAsList()
        val users = usersQueries.selectAll().awaitAsList()
        val receivedItems = receivedItemsQueries.selectAll().awaitAsList()
        return lendings.map { lending ->
            val relatedItems = items.filter { it.lendingId == lending.id }
            lending.toLending(relatedItems, inventoryItems, inventoryItemTypes, users, receivedItems)
        }
    }

    override suspend fun insert(item: ReferencedLending): Long {
        queries.insert(
            id = item.id,
            userSub = item.user.sub,
            timestamp = item.timestamp,
            fromDate = item.from,
            toDate = item.to,
            notes = item.notes,
            confirmed = item.confirmed,
            taken = item.taken,
            givenBy = item.givenBy?.sub,
            givenAt = item.givenAt,
            returned = item.returned,
            memorySubmitted = item.memorySubmitted,
            memorySubmittedAt = item.memorySubmittedAt,
            memoryDocumentId = item.memoryDocument,
            memoryPlainText = item.memoryPlainText,
            memoryReviewed = item.memoryReviewed,
        )
        for (inventoryItem in item.items) {
            val exists = lendingItemsQueries.get(item.id, inventoryItem.id).executeAsOneOrNull() != null
            if (!exists) {
                lendingItemsQueries.insert(
                    lendingId = item.id,
                    itemId = inventoryItem.id
                )
            }
        }
        for (receivedItem in item.receivedItems) {
            val exists = receivedItemsQueries.get(receivedItem.id).executeAsOneOrNull() != null
            if (!exists) {
                receivedItemsQueries.insert(
                    id = receivedItem.id,
                    lending = item.id,
                    item = receivedItem.itemId,
                    notes = receivedItem.notes,
                    receivedBy = receivedItem.receivedBy,
                    receivedAt = receivedItem.receivedAt,
                )
            }
        }
        return 1L
    }

    override suspend fun update(item: ReferencedLending): Long {
        queries.update(
            id = item.id,
            userSub = item.user.sub,
            timestamp = item.timestamp,
            fromDate = item.from,
            toDate = item.to,
            notes = item.notes,
            confirmed = item.confirmed,
            taken = item.taken,
            givenBy = item.givenBy?.sub,
            givenAt = item.givenAt,
            returned = item.returned,
            memorySubmitted = item.memorySubmitted,
            memorySubmittedAt = item.memorySubmittedAt,
            memoryDocumentId = item.memoryDocument,
            memoryPlainText = item.memoryPlainText,
            memoryReviewed = item.memoryReviewed,
        )

        lendingItemsQueries.deleteByLendingId(item.id)
        for (inventoryItem in item.items) {
            lendingItemsQueries.insert(
                lendingId = item.id,
                itemId = inventoryItem.id
            )
        }

        receivedItemsQueries.deleteByLendingId(item.id)
        for (receivedItem in item.receivedItems) {
            receivedItemsQueries.insert(
                id = receivedItem.id,
                lending = item.id,
                item = receivedItem.itemId,
                notes = receivedItem.notes,
                receivedBy = receivedItem.receivedBy,
                receivedAt = receivedItem.receivedAt,
            )
        }

        return 1L
    }

    override suspend fun delete(id: Uuid) {
        queries.deleteById(id)
    }

    fun ReceivedItems.toReceivedItem(): ReceivedItem = ReceivedItem(
        id = id,
        lendingId = lending,
        itemId = item,
        notes = notes,
        receivedBy = receivedBy,
        receivedAt = receivedAt,
    )

    fun Lendings.toLending(
        items: List<LendingItems>,
        inventoryItems: List<InventoryItems>,
        inventoryItemTypes: List<InventoryItemTypes>,
        users: List<Users>,
        receivedItems: List<ReceivedItems>,
    ) = Lending(
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
        memorySubmitted = memorySubmitted,
        memorySubmittedAt = memorySubmittedAt,
        memoryDocument = memoryDocumentId,
        memoryReviewed = memoryReviewed,
        memoryPlainText = memoryPlainText,
        items = items.mapNotNull { item -> inventoryItems.find { it.id == item.itemId }?.toInventoryItem() },
        receivedItems = receivedItems.filter { it.lending == id }.map { it.toReceivedItem() }
    ).referenced(
        users.map { it.toUser() },
        inventoryItemTypes.map { it.toInventoryItemType() }
    )
}
