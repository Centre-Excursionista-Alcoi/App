package org.centrexcursionistalcoi.app.database

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.centrexcursionistalcoi.app.data.ReferencedLending
import org.centrexcursionistalcoi.app.database.room.entity.LendingEntity.Companion.toEntity
import org.centrexcursionistalcoi.app.database.room.entity.LendingItemEntity
import org.centrexcursionistalcoi.app.database.room.entity.ReceivedItemEntity.Companion.toEntity
import org.centrexcursionistalcoi.app.database.room.relation.toReferenced
import org.koin.core.annotation.Singleton
import kotlin.uuid.Uuid

@Singleton
class LendingsRepository(
    private val db: AppDatabase,
    private val memoriesRepository: MemoriesRepository,
) : Repository<ReferencedLending, Uuid> {
    private val dao = db.lendingDao()

    override suspend fun get(id: Uuid): ReferencedLending? = dao.get(id)?.toReferenced()

    override fun getAsFlow(id: Uuid): Flow<ReferencedLending?> = dao.getAsFlow(id).map { it?.toReferenced() }

    override fun selectAllAsFlow(): Flow<List<ReferencedLending>> = dao.selectAllAsFlow().map { list -> list.map { it.toReferenced() } }

    override suspend fun selectAll(): List<ReferencedLending> = dao.selectAll().map { it.toReferenced() }

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

}
