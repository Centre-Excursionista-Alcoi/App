package org.centrexcursionistalcoi.app.database

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.centrexcursionistalcoi.app.data.Memory
import org.centrexcursionistalcoi.app.data.ReferencedMemory
import org.centrexcursionistalcoi.app.database.entity.MemoryEntity
import org.centrexcursionistalcoi.app.database.entity.MemoryEntity.Companion.toEntity
import org.centrexcursionistalcoi.app.database.entity.MemoryMemberCrossRef
import org.centrexcursionistalcoi.app.database.relation.toReferenced
import org.koin.core.annotation.Singleton
import kotlin.uuid.Uuid

/**
 * Local storage for memories.
 *
 * Memories are their own resource on the server, and only optionally reference a lending (see [Memory.lending]) --
 * this repository stores them independently of [LendingsRepository], regardless of whether they are linked to a
 * lending or not.
 */
@Singleton
class MemoriesRepository(
    private val db: AppDatabase,
) : Repository<ReferencedMemory, Uuid> {
    private val dao = db.memoryDao()

    override suspend fun get(id: Uuid): ReferencedMemory? = dao.get(id)?.toReferenced()

    /** Returns the raw memory linked to the lending with the given [lendingId], if any. */
    suspend fun getByLendingId(lendingId: Uuid): ReferencedMemory? = dao.getByLendingId(lendingId).firstOrNull()?.toReferenced()

    override suspend fun getByIdList(ids: List<Uuid>): List<ReferencedMemory> = dao.getByIdList(ids).map { it.toReferenced() }

    override fun getAsFlow(id: Uuid): Flow<ReferencedMemory?> = dao.getAsFlow(id).map { it?.toReferenced() }

    override suspend fun selectAll(): List<ReferencedMemory> = dao.selectAll().map { it.toReferenced() }

    override fun selectAllAsFlow(): Flow<List<ReferencedMemory>> = dao.selectAllAsFlow().map { list -> list.map { it.toReferenced() } }

    override suspend fun insert(item: ReferencedMemory) = insertRaw(item.dereference())

    override suspend fun update(item: ReferencedMemory) = updateRaw(item.dereference())

    /** Inserts or updates the given raw [memory], without needing to resolve its members/department/submitter first. */
    suspend fun insertOrUpdate(memory: Memory) {
        if (dao.get(memory.id) != null) updateRaw(memory) else insertRaw(memory)
    }

    /**
     * Writes only the [MemoryEntity] row -- unlike [insertOrUpdate]/[insert]/[update] of [Memory]/[ReferencedMemory],
     * this does not touch [MemoryMemberCrossRef] rows.
     */
    suspend fun insert(item: MemoryEntity) = dao.insert(item)

    /** @see insert */
    suspend fun update(item: MemoryEntity) = dao.update(item)

    /** @see insert */
    suspend fun upsert(item: MemoryEntity) = dao.upsert(item)

    private suspend fun insertRaw(memory: Memory) {
        dao.insert(memory.toEntity())
        insertMemberCrossRefs(memory)
    }

    private suspend fun updateRaw(memory: Memory) {
        dao.update(memory.toEntity())
        val crossRefDao = db.memoryMemberCrossRefDao()
        crossRefDao.deleteByMemoryId(memory.id)
        insertMemberCrossRefs(memory)
    }

    private suspend fun insertMemberCrossRefs(memory: Memory) {
        val crossRefDao = db.memoryMemberCrossRefDao()
        for (memberNumber in memory.members) {
            crossRefDao.insert(
                MemoryMemberCrossRef(
                    memoryId = memory.id,
                    memberNumber = memberNumber.toLong()
                )
            )
        }
    }

    override suspend fun delete(id: Uuid) {
        dao.deleteById(id)
    }
}
