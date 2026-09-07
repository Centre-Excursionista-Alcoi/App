package org.centrexcursionistalcoi.app.database

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.centrexcursionistalcoi.app.data.Event
import org.centrexcursionistalcoi.app.data.ReferencedEvent
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

    override suspend fun insert(item: ReferencedEvent) {
        dao.insert(item.dereference().toEntity())
        insertUserCrossRefs(item.id, item.userSubList.map { it.sub })
    }

    override suspend fun update(item: ReferencedEvent) {
        dao.update(item.dereference().toEntity())
        db.eventUserCrossRefDao().deleteByEventId(item.id)
        insertUserCrossRefs(item.id, item.userSubList.map { it.sub })
    }

    /** Inserts the given raw [event] (including its [EventUserCrossRef] rows), without needing to resolve its department/users first. */
    suspend fun insertRaw(event: Event) {
        dao.insert(event.toEntity())
        insertUserCrossRefs(event.id, event.userSubList)
    }

    /** Updates the given raw [event] (including its [EventUserCrossRef] rows), without needing to resolve its department/users first. */
    suspend fun updateRaw(event: Event) {
        dao.update(event.toEntity())
        db.eventUserCrossRefDao().deleteByEventId(event.id)
        insertUserCrossRefs(event.id, event.userSubList)
    }

    /** Inserts or updates the given raw [event], without needing to resolve its department/users first. */
    suspend fun insertOrUpdate(event: Event) {
        if (dao.get(event.id) != null) updateRaw(event) else insertRaw(event)
    }

    private suspend fun insertUserCrossRefs(eventId: Uuid, subs: List<String>) {
        val crossRefDao = db.eventUserCrossRefDao()
        for (sub in subs) {
            crossRefDao.insert(
                EventUserCrossRef(
                    eventId = eventId,
                    userSub = sub
                )
            )
        }
    }

    override suspend fun delete(id: Uuid) {
        dao.deleteById(id)
    }
}
