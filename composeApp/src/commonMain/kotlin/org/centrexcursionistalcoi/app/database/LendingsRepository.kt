package org.centrexcursionistalcoi.app.database

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.centrexcursionistalcoi.app.data.Lending
import org.centrexcursionistalcoi.app.data.ReferencedLending
import org.centrexcursionistalcoi.app.data.ReferencedMemory
import org.centrexcursionistalcoi.app.database.entity.LendingEntity.Companion.toEntity
import org.centrexcursionistalcoi.app.database.entity.LendingItemEntity
import org.centrexcursionistalcoi.app.database.entity.ReceivedItemEntity.Companion.toEntity
import org.centrexcursionistalcoi.app.database.relation.toReferenced
import org.koin.core.annotation.Singleton
import kotlin.uuid.Uuid

@Singleton
class LendingsRepository(
    private val db: AppDatabase,
    private val memoriesRepository: MemoriesRepository,
) : Repository<ReferencedLending, Uuid> {
    private val dao = db.lendingDao()

    override suspend fun get(id: Uuid): ReferencedLending? = dao.get(id)?.toReferenced()

    override suspend fun getByIdList(ids: List<Uuid>): List<ReferencedLending> = dao.getByIdList(ids).map { it.toReferenced() }

    override fun getAsFlow(id: Uuid): Flow<ReferencedLending?> = dao.getAsFlow(id).map { it?.toReferenced() }

    override fun selectAllAsFlow(): Flow<List<ReferencedLending>> = dao.selectAllAsFlow().map { list -> list.map { it.toReferenced() } }

    override suspend fun selectAll(): List<ReferencedLending> = dao.selectAll().map { it.toReferenced() }

    override suspend fun insert(item: ReferencedLending) {
        dao.insert(item.dereference().toEntity())
        item.memory?.let { insertOrUpdateMemory(it) }

        val lendingItemDao = db.lendingItemDao()
        for (inventoryItem in item.items) {
            val exists = lendingItemDao.get(item.id, inventoryItem.id) != null
            if (!exists) {
                lendingItemDao.insert(
                    LendingItemEntity(
                        lendingId = item.id,
                        itemId = inventoryItem.id
                    )
                )
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
        item.memory?.let { insertOrUpdateMemory(it) }

        val lendingItemDao = db.lendingItemDao()
        lendingItemDao.deleteByLendingId(item.id)
        for (inventoryItem in item.items) {
            lendingItemDao.insert(
                LendingItemEntity(
                    lendingId = item.id,
                    itemId = inventoryItem.id
                )
            )
        }

        val receivedItemDao = db.receivedItemDao()
        receivedItemDao.deleteByLendingId(item.id)
        for (receivedItem in item.receivedItems) {
            receivedItemDao.insert(receivedItem.toEntity())
        }
    }

    private suspend fun insertOrUpdateMemory(memory: ReferencedMemory) {
        if (memoriesRepository.get(memory.id) == null) memoriesRepository.insert(memory) else memoriesRepository.update(memory)
    }

    /**
     * Inserts the given raw [lending] (including its [LendingItemEntity]/[org.centrexcursionistalcoi.app.database.entity.ReceivedItemEntity] rows),
     * without needing to resolve its items first. Unlike [insert] of [ReferencedLending], this does not persist [Lending.memory]
     * -- memories are their own resource, synced separately.
     */
    suspend fun insertRaw(lending: Lending) {
        dao.insert(lending.toEntity())

        val lendingItemDao = db.lendingItemDao()
        for (item in lending.items) {
            val exists = lendingItemDao.get(lending.id, item.id) != null
            if (!exists) {
                lendingItemDao.insert(
                    LendingItemEntity(
                        lendingId = lending.id,
                        itemId = item.id
                    )
                )
            }
        }

        val receivedItemDao = db.receivedItemDao()
        for (receivedItem in lending.receivedItems) {
            val exists = receivedItemDao.get(receivedItem.id) != null
            if (!exists) {
                receivedItemDao.insert(receivedItem.toEntity())
            }
        }
    }

    /** @see insertRaw */
    suspend fun updateRaw(lending: Lending) {
        dao.update(lending.toEntity())

        val lendingItemDao = db.lendingItemDao()
        lendingItemDao.deleteByLendingId(lending.id)
        for (item in lending.items) {
            lendingItemDao.insert(
                LendingItemEntity(
                    lendingId = lending.id,
                    itemId = item.id
                )
            )
        }

        val receivedItemDao = db.receivedItemDao()
        receivedItemDao.deleteByLendingId(lending.id)
        for (receivedItem in lending.receivedItems) {
            receivedItemDao.insert(receivedItem.toEntity())
        }
    }

    /** Inserts or updates the given raw [lending]. @see insertRaw */
    suspend fun insertOrUpdate(lending: Lending) {
        if (dao.get(lending.id) != null) updateRaw(lending) else insertRaw(lending)
    }

    override suspend fun delete(id: Uuid) {
        dao.deleteById(id)
    }

}
