package org.centrexcursionistalcoi.app.network

import kotlin.uuid.Uuid
import org.centrexcursionistalcoi.app.data.Event
import org.centrexcursionistalcoi.app.data.ReferencedEvent
import org.centrexcursionistalcoi.app.data.ReferencedEvent.Companion.referenced
import org.centrexcursionistalcoi.app.database.DepartmentsRepository
import org.centrexcursionistalcoi.app.database.EventsRepository
import org.centrexcursionistalcoi.app.database.UsersRepository
import org.centrexcursionistalcoi.app.storage.SETTINGS_LAST_EVENTS_SYNC

object EventsRemoteRepository : RemoteRepository<Uuid, ReferencedEvent, Uuid, Event>(
    "/events",
    SETTINGS_LAST_EVENTS_SYNC,
    Event.serializer(),
    EventsRepository,
    remoteToLocalIdConverter = { it },
    remoteToLocalEntityConverter = { event ->
        val departments = DepartmentsRepository.selectAll()
        val users = UsersRepository.selectAll()
        event.referenced(departments, users)
    },
)
