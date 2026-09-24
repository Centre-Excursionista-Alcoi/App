package org.centrexcursionistalcoi.app.database

import com.diamondedge.logging.logging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.centrexcursionistalcoi.app.data.Memory
import org.centrexcursionistalcoi.app.data.ReferencedMemory
import org.centrexcursionistalcoi.app.database.entity.MemoryEntity.Companion.toEntity
import org.centrexcursionistalcoi.app.database.entity.MemoryMemberCrossRef
import org.centrexcursionistalcoi.app.database.relation.MemoryWithRelations
import org.centrexcursionistalcoi.app.database.relation.toReferenced
import org.centrexcursionistalcoi.app.exception.MissingCrossReferenceException
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
    private val log = logging()

    /**
     * [MemoryWithRelations.toReferenced] throws [MissingCrossReferenceException] when the memory's submitter
     * isn't resolvable locally yet -- expected for [DatabaseIntegrityVerifier][org.centrexcursionistalcoi.app.sync.DatabaseIntegrityVerifier],
     * which relies on the throw to detect and repair it, but fatal (crashes the whole collecting Flow --
     * confirmed via an iOS crash report, uncaught inside a live Room Flow's `.map`) for every read below, which
     * observe rows the moment a background sync inserts them, before the integrity verifier's next pass gets a
     * chance to backfill a placeholder user for them. Skipping the row here is safe: it reappears once the
     * reference resolves, either through that repair or a later sync.
     */
    private fun MemoryWithRelations.toReferencedOrNull(): ReferencedMemory? = try {
        toReferenced()
    } catch (e: MissingCrossReferenceException) {
        log.w(e) { "Skipping memory ${memory.id}: missing cross-reference." }
        null
    }

    override suspend fun get(id: Uuid): ReferencedMemory? = dao.get(id)?.toReferencedOrNull()

    /** Returns the raw memory linked to the lending with the given [lendingId], if any. */
    suspend fun getByLendingId(lendingId: Uuid): ReferencedMemory? = dao.getByLendingId(lendingId)?.toReferencedOrNull()

    override suspend fun getByIdList(ids: List<Uuid>): List<ReferencedMemory> = dao.getByIdList(ids).mapNotNull { it.toReferencedOrNull() }

    override fun getAsFlow(id: Uuid): Flow<ReferencedMemory?> = dao.getAsFlow(id).map { it?.toReferencedOrNull() }

    /** Returns the raw memory linked to the lending with the given [lendingId], if any. */
    fun getByLendingIdAsFlow(lendingId: Uuid): Flow<ReferencedMemory?> = dao.getByLendingIdAsFlow(lendingId).map { it?.toReferencedOrNull() }

    override suspend fun selectAll(): List<ReferencedMemory> = dao.selectAll().mapNotNull { it.toReferencedOrNull() }

    override fun selectAllAsFlow(): Flow<List<ReferencedMemory>> = dao.selectAllAsFlow().map { list -> list.mapNotNull { it.toReferencedOrNull() } }

    override suspend fun insert(item: ReferencedMemory) = insertRaw(item.dereference())

    override suspend fun update(item: ReferencedMemory) = updateRaw(item.dereference())

    /** Inserts or updates the given raw [memory], without needing to resolve its members/department/submitter first. */
    suspend fun insertOrUpdate(memory: Memory) {
        if (dao.get(memory.id) != null) updateRaw(memory) else insertRaw(memory)
    }

    /** Inserts the given raw [memory] (including its [MemoryMemberCrossRef] rows), without needing to resolve its members/department/submitter first. */
    suspend fun insertRaw(memory: Memory) {
        dao.insert(memory.toEntity())
        insertMemberCrossRefs(memory)
    }

    /** Updates the given raw [memory] (including its [MemoryMemberCrossRef] rows), without needing to resolve its members/department/submitter first. */
    suspend fun updateRaw(memory: Memory) {
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
