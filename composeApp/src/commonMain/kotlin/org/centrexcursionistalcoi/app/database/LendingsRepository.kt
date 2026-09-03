package org.centrexcursionistalcoi.app.database

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.data.InventoryItem
import org.centrexcursionistalcoi.app.data.Member
import org.centrexcursionistalcoi.app.data.ReferencedInventoryItemType.Companion.referenced
import org.centrexcursionistalcoi.app.data.ReferencedLending
import org.centrexcursionistalcoi.app.data.ReferencedMemory
import org.centrexcursionistalcoi.app.data.referenced
import org.centrexcursionistalcoi.app.database.room.entity.InventoryItemEntity
import org.centrexcursionistalcoi.app.database.room.entity.InventoryItemTypeEntity
import org.centrexcursionistalcoi.app.database.room.entity.LendingEntity
import org.centrexcursionistalcoi.app.database.room.entity.LendingEntity.Companion.toEntity
import org.centrexcursionistalcoi.app.database.room.entity.LendingItemEntity
import org.centrexcursionistalcoi.app.database.room.entity.ReceivedItemEntity
import org.centrexcursionistalcoi.app.database.room.entity.ReceivedItemEntity.Companion.toEntity
import org.centrexcursionistalcoi.app.database.room.entity.UserEntity
import org.koin.core.annotation.Singleton
import kotlin.uuid.Uuid

@Singleton
class LendingsRepository(
    private val db: AppDatabase,
    private val departmentsRepository: DepartmentsRepository,
    private val memoriesRepository: MemoriesRepository,
    private val membersRepository: MembersRepository,
) : Repository<ReferencedLending, Uuid> {
    private val dao = db.lendingDao()

    override suspend fun get(id: Uuid): ReferencedLending? {
        val lending = dao.get(id) ?: return null
        val lendingItems = db.lendingItemDao().getByLendingId(id)
        val inventoryItems = db.inventoryItemDao().selectAll().map { it.toInventoryItem() }
        val inventoryItemTypes = db.inventoryItemTypeDao().selectAll()
        val users = db.userDao().selectAll().map { it.toUser() }
        val receivedItems = db.receivedItemDao().selectAll()
        val departments = departmentsRepository.selectAll()
        val memory = memoriesRepository.getByLendingId(id)
        val members = membersRepository.selectAll()
        return lending.buildLending(lendingItems, inventoryItems, receivedItems, id, memory?.id).referenced(
            users,
            inventoryItemTypes.toReferencedTypes(departments),
            memory,
            members,
            departments,
        )
    }

    override fun getAsFlow(id: Uuid): Flow<ReferencedLending?> {
        val lendingFlow = dao.getAsFlow(id)
        val lendingItemsFlow = db.lendingItemDao().getByLendingIdAsFlow(id)
        val inventoryItemsFlow = db.inventoryItemDao().selectAllAsFlow()
        val inventoryItemTypesFlow = db.inventoryItemTypeDao().selectAllAsFlow()
        val usersFlow = db.userDao().selectAllAsFlow()
        val receivedItemsFlow = db.receivedItemDao().selectAllAsFlow()
        val departmentsFlow = departmentsRepository.selectAllAsFlow()
        val memoriesFlow = memoriesRepository.selectAllAsFlow()
        val membersFlow = membersRepository.selectAllAsFlow()
        @Suppress("UNCHECKED_CAST")
        return combine(
            lendingFlow,
            lendingItemsFlow,
            inventoryItemsFlow,
            inventoryItemTypesFlow,
            usersFlow,
            receivedItemsFlow,
            departmentsFlow,
            memoriesFlow,
            membersFlow,
        ) { flows ->
            val lending = flows[0] as LendingEntity?
            val lendingItems = flows[1] as List<LendingItemEntity>
            val inventoryItems = (flows[2] as List<InventoryItemEntity>).map { it.toInventoryItem() }
            val inventoryItemTypes = flows[3] as List<InventoryItemTypeEntity>
            val users = (flows[4] as List<UserEntity>).map { it.toUser() }
            val receivedItems = flows[5] as List<ReceivedItemEntity>
            val departments = flows[6] as List<Department>
            val memories = flows[7] as List<ReferencedMemory>
            val members = flows[8] as List<Member>
            val memory = memories.find { it.dereference().lending == id }?.dereference()
            lending?.buildLending(lendingItems, inventoryItems, receivedItems, id, memory?.id)?.referenced(
                users,
                inventoryItemTypes.toReferencedTypes(departments),
                memory,
                members,
                departments,
            )
        }
    }

    override fun selectAllAsFlow(): Flow<List<ReferencedLending>> {
        val lendingsFlow = dao.selectAllAsFlow()
        val lendingItemsFlow = db.lendingItemDao().selectAllAsFlow()
        val inventoryItemsFlow = db.inventoryItemDao().selectAllAsFlow()
        val inventoryItemTypesFlow = db.inventoryItemTypeDao().selectAllAsFlow()
        val usersFlow = db.userDao().selectAllAsFlow()
        val receivedItemsFlow = db.receivedItemDao().selectAllAsFlow()
        val departmentsFlow = departmentsRepository.selectAllAsFlow()
        val memoriesFlow = memoriesRepository.selectAllAsFlow()
        val membersFlow = membersRepository.selectAllAsFlow()
        @Suppress("UNCHECKED_CAST")
        return combine(
            lendingsFlow,
            lendingItemsFlow,
            inventoryItemsFlow,
            inventoryItemTypesFlow,
            usersFlow,
            receivedItemsFlow,
            departmentsFlow,
            memoriesFlow,
            membersFlow,
        ) { flows ->
            val lendings = flows[0] as List<LendingEntity>
            val lendingItems = flows[1] as List<LendingItemEntity>
            val inventoryItems = (flows[2] as List<InventoryItemEntity>).map { it.toInventoryItem() }
            val inventoryItemTypes = flows[3] as List<InventoryItemTypeEntity>
            val users = (flows[4] as List<UserEntity>).map { it.toUser() }
            val receivedItems = flows[5] as List<ReceivedItemEntity>
            val departments = flows[6] as List<Department>
            val memories = flows[7] as List<ReferencedMemory>
            val members = flows[8] as List<Member>
            val referencedTypes = inventoryItemTypes.toReferencedTypes(departments)
            lendings.map { lending ->
                val memory = memories.find { it.dereference().lending == lending.id }?.dereference()
                lending.buildLending(lendingItems, inventoryItems, receivedItems, lending.id, memory?.id).referenced(
                    users,
                    referencedTypes,
                    memory,
                    members,
                    departments,
                )
            }
        }
    }

    override suspend fun selectAll(): List<ReferencedLending> {
        val lendings = dao.selectAll()
        val lendingItems = db.lendingItemDao().selectAll()
        val inventoryItems = db.inventoryItemDao().selectAll().map { it.toInventoryItem() }
        val inventoryItemTypes = db.inventoryItemTypeDao().selectAll()
        val users = db.userDao().selectAll().map { it.toUser() }
        val receivedItems = db.receivedItemDao().selectAll()
        val departments = departmentsRepository.selectAll()
        val memories = memoriesRepository.selectAll()
        val members = membersRepository.selectAll()
        val referencedTypes = inventoryItemTypes.toReferencedTypes(departments)
        return lendings.map { lending ->
            val memory = memories.find { it.dereference().lending == lending.id }?.dereference()
            lending.buildLending(lendingItems, inventoryItems, receivedItems, lending.id, memory?.id).referenced(
                users,
                referencedTypes,
                memory,
                members,
                departments,
            )
        }
    }

    override suspend fun insert(item: ReferencedLending) {
        dao.insert(item.dereference().toEntity())
        item.memory?.let { memoriesRepository.insertOrUpdate(it) }

        val lendingItemDao = db.lendingItemDao()
        for (inventoryItem in item.items) {
            val exists = lendingItemDao.get(item.id, inventoryItem.id) != null
            if (!exists) {
                lendingItemDao.insert(LendingItemEntity(lendingId = item.id, itemId = inventoryItem.id))
            }
        }

        val receivedItemDao = db.receivedItemDao()
        for (receivedItem in item.receivedItems) {
            val exists = receivedItemDao.get(receivedItem.id) != null
            if (!exists) {
                receivedItemDao.insert(receivedItem.toEntity())
            }
        }
    }

    override suspend fun update(item: ReferencedLending) {
        dao.update(item.dereference().toEntity())
        item.memory?.let { memoriesRepository.insertOrUpdate(it) }

        val lendingItemDao = db.lendingItemDao()
        lendingItemDao.deleteByLendingId(item.id)
        for (inventoryItem in item.items) {
            lendingItemDao.insert(LendingItemEntity(lendingId = item.id, itemId = inventoryItem.id))
        }

        val receivedItemDao = db.receivedItemDao()
        receivedItemDao.deleteByLendingId(item.id)
        for (receivedItem in item.receivedItems) {
            receivedItemDao.insert(receivedItem.toEntity())
        }
    }

    override suspend fun delete(id: Uuid) {
        dao.deleteById(id)
    }

    private fun List<InventoryItemTypeEntity>.toReferencedTypes(departments: List<Department>) =
        map { it.toInventoryItemType().referenced(departments) }

    private fun LendingEntity.buildLending(
        lendingItems: List<LendingItemEntity>,
        inventoryItems: List<InventoryItem>,
        receivedItems: List<ReceivedItemEntity>,
        lendingId: Uuid,
        memoryId: Uuid?,
    ) = toLending(
        items = lendingItems
            .filter { it.lendingId == lendingId }
            .mapNotNull { li -> inventoryItems.find { it.id == li.itemId } },
        receivedItems = receivedItems.filter { it.lending == lendingId }.map { it.toReceivedItem() },
        memoryId = memoryId,
    )
}
