package org.centrexcursionistalcoi.app.database

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.centrexcursionistalcoi.app.data.ReferencedEvent
import org.centrexcursionistalcoi.app.data.ReferencedEvent.Companion.referenced
import org.centrexcursionistalcoi.app.database.room.entity.EventEntity.Companion.toEntity
import kotlin.uuid.Uuid

class EventsRepository(
    db: AppDatabase,
    private val departmentsRepository: DepartmentsRepository,
    private val usersRepository: UsersRepository,
) : Repository<ReferencedEvent, Uuid> {
    private val dao = db.eventDao()

    override suspend fun get(id: Uuid): ReferencedEvent? {
        val departments = departmentsRepository.selectAll()
        val users = usersRepository.selectAll()
        return dao.get(id)?.toEvent()?.referenced(departments, users)
    }

    override fun getAsFlow(id: Uuid): Flow<ReferencedEvent?> {
        val departmentsFlow = departmentsRepository.selectAllAsFlow()
        val usersFlow = usersRepository.selectAllAsFlow()
        val eventFlow = dao.getAsFlow(id)
        return combine(departmentsFlow, usersFlow, eventFlow) { departments, users, event ->
            event?.toEvent()?.referenced(departments, users)
        }
    }

    override fun selectAllAsFlow(): Flow<List<ReferencedEvent>> {
        val departmentsFlow = departmentsRepository.selectAllAsFlow()
        val usersFlow = usersRepository.selectAllAsFlow()
        val eventsFlow = dao.selectAllAsFlow()
        return combine(departmentsFlow, usersFlow, eventsFlow) { departments, users, events ->
            events.map { it.toEvent().referenced(departments, users) }
        }
    }

    override suspend fun selectAll(): List<ReferencedEvent> {
        val departments = departmentsRepository.selectAll()
        val users = usersRepository.selectAll()
        return dao.selectAll().map { it.toEvent().referenced(departments, users) }
    }

    override suspend fun insert(item: ReferencedEvent) = dao.insert(
        item.dereference().toEntity()
    )

    override suspend fun update(item: ReferencedEvent) = dao.update(
        item.dereference().toEntity()
    )

    override suspend fun delete(id: Uuid) {
        dao.deleteById(id)
    }
}
