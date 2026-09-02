package org.centrexcursionistalcoi.app.database

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.centrexcursionistalcoi.app.data.Memory
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
object MemoriesRepository : DatabaseRepository<Memory, Uuid>() {
    override val queries by lazy { databaseInstance.memoriesQueries }

    override fun selectAllAsFlow(dispatcher: CoroutineDispatcher): Flow<List<Memory>> = queries
        .selectAll()
        .asFlow()
        .mapToList(dispatcher)
        .map { list -> list.map { it.toMemory() } }

    override suspend fun get(id: Uuid): Memory? = queries.get(id).awaitAsList().firstOrNull()?.toMemory()

    /** Returns the memory linked to the lending with the given [lendingId], if any. */
    suspend fun getByLendingId(lendingId: Uuid): Memory? = queries.getByLendingId(lendingId).awaitAsList().firstOrNull()?.toMemory()

    override fun getAsFlow(id: Uuid, dispatcher: CoroutineDispatcher): Flow<Memory?> = queries
        .get(id)
        .asFlow()
        .mapToList(dispatcher)
        .map { it.firstOrNull()?.toMemory() }

    override suspend fun selectAll(): List<Memory> = queries.selectAll().awaitAsList().map { it.toMemory() }

    override suspend fun insert(item: Memory) = queries.insert(
        id = item.id,
        place = item.place,
        members = item.members,
        externalUsers = item.externalUsers,
        text = item.text,
        sport = item.sport,
        department = item.department,
        attachments = item.attachments,
        submittedBy = item.submittedBy,
        fromDate = item.from,
        toDate = item.to,
        pdf = item.pdf,
        lending = item.lending,
    )

    override suspend fun update(item: Memory) = queries.update(
        place = item.place,
        members = item.members,
        externalUsers = item.externalUsers,
        text = item.text,
        sport = item.sport,
        department = item.department,
        attachments = item.attachments,
        submittedBy = item.submittedBy,
        fromDate = item.from,
        toDate = item.to,
        pdf = item.pdf,
        lending = item.lending,
        id = item.id,
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
