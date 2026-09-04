package org.centrexcursionistalcoi.app.database

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.centrexcursionistalcoi.app.data.ReferencedEvent
import org.centrexcursionistalcoi.app.database.entity.EventEntity
import org.centrexcursionistalcoi.app.database.entity.EventEntity.Companion.toEntity
import org.centrexcursionistalcoi.app.database.entity.EventUserCrossRef
import org.centrexcursionistalcoi.app.database.relation.toReferenced
import org.koin.core.annotation.Singleton
import kotlin.uuid.Uuid

@Singleton
class EventsRepository(private val db: AppDatabase) : Repository<ReferencedEvent, Uuid> {
    private val dao = db.eventDao()

    override suspend fun get(id: Uuid): ReferencedEvent? = dao.get(id)?.toReferenced()

    override suspend fun getByIdList(ids: List<Uuid>): List<ReferencedEvent> = dao.getByIdList(ids).map { it.toReferenced() }

    override fun getAsFlow(id: Uuid): Flow<ReferencedEvent?> = dao.getAsFlow(id).map { it?.toReferenced() }

    override fun selectAllAsFlow(): Flow<List<ReferencedEvent>> = dao.selectAllAsFlow().map { list -> list.map { it.toReferenced() } }

    override suspend fun selectAll(): List<ReferencedEvent> = dao.selectAll().map { it.toReferenced() }

    /** Writes only the [EventEntity] row -- unlike [insert]/[update] of [ReferencedEvent], this does not touch [EventUserCrossRef] rows. */
    suspend fun insert(item: EventEntity) = dao.insert(item)

    override suspend fun insert(item: ReferencedEvent) {
        dao.insert(item.dereference().toEntity())
        insertUserCrossRefs(item)
    }

    /** @see insert */
    suspend fun update(item: EventEntity) = dao.update(item)

    override suspend fun update(item: ReferencedEvent) {
        dao.update(item.dereference().toEntity())
        val crossRefDao = db.eventUserCrossRefDao()
        crossRefDao.deleteByEventId(item.id)
        insertUserCrossRefs(item)
    }

    /** @see insert */
    suspend fun upsert(item: EventEntity) = dao.upsert(item)

    private suspend fun insertUserCrossRefs(item: ReferencedEvent) {
        val crossRefDao = db.eventUserCrossRefDao()
        for (user in item.userSubList) {
            crossRefDao.insert(
                EventUserCrossRef(
                    eventId = item.id,
                    userSub = user.sub
                )
            )
        }
    }

    override suspend fun delete(id: Uuid) {
        dao.deleteById(id)
    }
}
