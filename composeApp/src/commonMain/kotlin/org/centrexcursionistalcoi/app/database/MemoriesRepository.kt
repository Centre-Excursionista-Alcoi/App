package org.centrexcursionistalcoi.app.database

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.centrexcursionistalcoi.app.data.Memory
import org.centrexcursionistalcoi.app.data.ReferencedMemory
import org.centrexcursionistalcoi.app.database.data.Memories
import org.centrexcursionistalcoi.app.storage.databaseInstance
import kotlin.uuid.Uuid

/**
 * Local storage for memories.
 *
 * Memories are their own resource on the server, and only optionally reference a lending (see [Memory.lending]) --
 * this repository stores them independently of [LendingsRepository], regardless of whether they are linked to a
 * lending or not.
 */
object MemoriesRepository : DatabaseRepository<ReferencedMemory, Uuid>() {
    override val queries by lazy { databaseInstance.memoriesQueries }

    override suspend fun get(id: Uuid): ReferencedMemory? {
        val memory = getRaw(id) ?: return null
        return memory.referenced(UsersRepository.selectAll(), MembersRepository.selectAll(), DepartmentsRepository.selectAll())
    }

    private suspend fun getRaw(id: Uuid): Memory? = queries.get(id).awaitAsList().firstOrNull()?.toMemory()

    /** Returns the raw memory linked to the lending with the given [lendingId], if any. */
    suspend fun getByLendingId(lendingId: Uuid): Memory? = queries.getByLendingId(lendingId).awaitAsList().firstOrNull()?.toMemory()

    override fun getAsFlow(id: Uuid, dispatcher: CoroutineDispatcher): Flow<ReferencedMemory?> = combine(
        queries.get(id).asFlow().mapToList(dispatcher),
        UsersRepository.selectAllAsFlow(dispatcher),
        MembersRepository.selectAllAsFlow(dispatcher),
        DepartmentsRepository.selectAllAsFlow(dispatcher),
    ) { rows, users, members, departments ->
        rows.firstOrNull()?.toMemory()?.referenced(users, members, departments)
    }

    override suspend fun selectAll(): List<ReferencedMemory> {
        val users = UsersRepository.selectAll()
        val members = MembersRepository.selectAll()
        val departments = DepartmentsRepository.selectAll()
        return queries.selectAll().awaitAsList().map { it.toMemory().referenced(users, members, departments) }
    }

    override fun selectAllAsFlow(dispatcher: CoroutineDispatcher): Flow<List<ReferencedMemory>> = combine(
        queries.selectAll().asFlow().mapToList(dispatcher),
        UsersRepository.selectAllAsFlow(dispatcher),
        MembersRepository.selectAllAsFlow(dispatcher),
        DepartmentsRepository.selectAllAsFlow(dispatcher),
    ) { rows, users, members, departments ->
        rows.map { it.toMemory().referenced(users, members, departments) }
    }

    override suspend fun insert(item: ReferencedMemory) = insertRaw(item.dereference())

    override suspend fun update(item: ReferencedMemory) = updateRaw(item.dereference())

    /** Inserts or updates the given raw [memory], without needing to resolve its members/department/submitter first. */
    suspend fun insertOrUpdate(memory: Memory) {
        if (getRaw(memory.id) != null) updateRaw(memory) else insertRaw(memory)
    }

    private suspend fun insertRaw(memory: Memory) = queries.insert(
        id = memory.id,
        place = memory.place,
        members = memory.members,
        externalUsers = memory.externalUsers,
        text = memory.text,
        sport = memory.sport,
        department = memory.department,
        attachments = memory.attachments,
        submittedBy = memory.submittedBy,
        fromDate = memory.from,
        toDate = memory.to,
        pdf = memory.pdf,
        lending = memory.lending,
    )

    private suspend fun updateRaw(memory: Memory) = queries.update(
        place = memory.place,
        members = memory.members,
        externalUsers = memory.externalUsers,
        text = memory.text,
        sport = memory.sport,
        department = memory.department,
        attachments = memory.attachments,
        submittedBy = memory.submittedBy,
        fromDate = memory.from,
        toDate = memory.to,
        pdf = memory.pdf,
        lending = memory.lending,
        id = memory.id,
    )

    override suspend fun delete(id: Uuid) {
        queries.deleteById(id)
    }

    fun Memories.toMemory() = Memory(
        id = id,
        place = place,
        members = members.orEmpty(),
        externalUsers = externalUsers,
        text = text,
        sport = sport,
        department = department,
        attachments = attachments.orEmpty(),
        submittedBy = submittedBy,
        from = fromDate,
        to = toDate,
        pdf = pdf,
        lending = lending,
    )
}
