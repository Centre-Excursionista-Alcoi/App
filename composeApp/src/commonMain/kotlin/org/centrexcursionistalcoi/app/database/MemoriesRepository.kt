package org.centrexcursionistalcoi.app.database

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.centrexcursionistalcoi.app.data.Memory
import org.centrexcursionistalcoi.app.data.ReferencedMemory
import org.centrexcursionistalcoi.app.database.room.entity.MemoryEntity.Companion.toEntity
import kotlin.uuid.Uuid

/**
 * Local storage for memories.
 *
 * Memories are their own resource on the server, and only optionally reference a lending (see [Memory.lending]) --
 * this repository stores them independently of [LendingsRepository], regardless of whether they are linked to a
 * lending or not.
 */
class MemoriesRepository(
    db: AppDatabase,
    private val usersRepository: UsersRepository,
    private val membersRepository: MembersRepository,
    private val departmentsRepository: DepartmentsRepository,
) : Repository<ReferencedMemory, Uuid> {
    private val dao = db.memoryDao()

    override suspend fun get(id: Uuid): ReferencedMemory? {
        val memory = getRaw(id) ?: return null
        return memory.referenced(usersRepository.selectAll(), membersRepository.selectAll(), departmentsRepository.selectAll())
    }

    private suspend fun getRaw(id: Uuid): Memory? = dao.get(id)?.toMemory()

    /** Returns the raw memory linked to the lending with the given [lendingId], if any. */
    suspend fun getByLendingId(lendingId: Uuid): Memory? = dao.getByLendingId(lendingId).firstOrNull()?.toMemory()

    override fun getAsFlow(id: Uuid): Flow<ReferencedMemory?> = combine(
        dao.getAsFlow(id),
        usersRepository.selectAllAsFlow(),
        membersRepository.selectAllAsFlow(),
        departmentsRepository.selectAllAsFlow(),
    ) { entity, users, members, departments ->
        entity?.toMemory()?.referenced(users, members, departments)
    }

    override suspend fun selectAll(): List<ReferencedMemory> {
        val users = usersRepository.selectAll()
        val members = membersRepository.selectAll()
        val departments = departmentsRepository.selectAll()
        return dao.selectAll().map { it.toMemory().referenced(users, members, departments) }
    }

    override fun selectAllAsFlow(): Flow<List<ReferencedMemory>> = combine(
        dao.selectAllAsFlow(),
        usersRepository.selectAllAsFlow(),
        membersRepository.selectAllAsFlow(),
        departmentsRepository.selectAllAsFlow(),
    ) { entities, users, members, departments ->
        entities.map { it.toMemory().referenced(users, members, departments) }
    }

    override suspend fun insert(item: ReferencedMemory) = insertRaw(item.dereference())

    override suspend fun update(item: ReferencedMemory) = updateRaw(item.dereference())

    /** Inserts or updates the given raw [memory], without needing to resolve its members/department/submitter first. */
    suspend fun insertOrUpdate(memory: Memory) {
        if (getRaw(memory.id) != null) updateRaw(memory) else insertRaw(memory)
    }

    private suspend fun insertRaw(memory: Memory) = dao.insert(memory.toEntity())

    private suspend fun updateRaw(memory: Memory) = dao.update(memory.toEntity())

    override suspend fun delete(id: Uuid) {
        dao.deleteById(id)
    }
}
