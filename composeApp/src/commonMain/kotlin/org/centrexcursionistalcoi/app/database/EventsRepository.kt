package org.centrexcursionistalcoi.app.database

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.data.Event
import org.centrexcursionistalcoi.app.data.ReferencedEvent
import org.centrexcursionistalcoi.app.data.ReferencedEvent.Companion.referenced
import org.centrexcursionistalcoi.app.data.UserData
import org.centrexcursionistalcoi.app.database.data.Events
import org.centrexcursionistalcoi.app.storage.databaseInstance

object EventsRepository : DatabaseRepository<ReferencedEvent, Uuid>() {
    override val queries by lazy { databaseInstance.eventsQueries }

    override suspend fun get(id: Uuid): ReferencedEvent? {
        val departments = DepartmentsRepository.selectAll()
        val users = UsersRepository.selectAll()
        return queries.get(id).awaitAsList().firstOrNull()?.toEvent(departments, users)
    }

    override fun getAsFlow(id: Uuid, dispatcher: CoroutineDispatcher): Flow<ReferencedEvent?> {
        val departmentsFlow = DepartmentsRepository.selectAllAsFlow(dispatcher)
        val usersFlow = UsersRepository.selectAllAsFlow(dispatcher)
        val eventsFlow = queries.get(id).asFlow().mapToList(dispatcher)
        return combine(departmentsFlow, usersFlow, eventsFlow) { departments, users, events ->
            events.firstOrNull()?.toEvent(departments, users)
        }
    }

    override fun selectAllAsFlow(dispatcher: CoroutineDispatcher): Flow<List<ReferencedEvent>> {
        val departmentsFlow = DepartmentsRepository.selectAllAsFlow(dispatcher)
        val usersFlow = UsersRepository.selectAllAsFlow(dispatcher)
        val eventsFlow = queries.selectAll().asFlow().mapToList(dispatcher)
        return combine(departmentsFlow, usersFlow, eventsFlow) { departments, users, events ->
            events.map { it.toEvent(departments, users) }
        }
    }

    override suspend fun selectAll(): List<ReferencedEvent> {
        val departments = DepartmentsRepository.selectAll()
        val users = UsersRepository.selectAll()
        return queries.selectAll().awaitAsList().map { it.toEvent(departments, users) }
    }

    override suspend fun insert(item: ReferencedEvent) = queries.insert(
        id = item.id,
        start = item.start,
        end = item.end,
        place = item.place,
        title = item.title,
        description = item.description,
        maxPeople = item.maxPeople?.toLong(),
        requiresConfirmation = item.requiresConfirmation,
        department = item.department?.id,
        image = item.image,
        userSubList = item.usersList.map { it.sub },
    )

    override suspend fun update(item: ReferencedEvent) = queries.update(
        start = item.start,
        end = item.end,
        place = item.place,
        title = item.title,
        description = item.description,
        maxPeople = item.maxPeople?.toLong(),
        requiresConfirmation = item.requiresConfirmation,
        department = item.department?.id,
        image = item.image,
        userSubList = item.usersList.map { it.sub },
        id = item.id,
    )

    override suspend fun delete(id: Uuid) {
        queries.deleteById(id)
    }

    fun Events.toEvent(departments: List<Department>, users: List<UserData>) = Event(
        id = id,
        start = start,
        end = end,
        place = place,
        title = title,
        description = description,
        maxPeople = maxPeople,
        requiresConfirmation = requiresConfirmation,
        department = department,
        image = image,
        userSubList = userSubList,
    ).referenced(departments, users)
}
