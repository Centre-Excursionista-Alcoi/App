package org.centrexcursionistalcoi.app.network

import io.github.vinceglb.filekit.PlatformFile
import kotlin.uuid.Uuid
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.centrexcursionistalcoi.app.data.Event
import org.centrexcursionistalcoi.app.data.ReferencedEvent
import org.centrexcursionistalcoi.app.data.ReferencedEvent.Companion.referenced
import org.centrexcursionistalcoi.app.database.DepartmentsRepository
import org.centrexcursionistalcoi.app.database.EventsRepository
import org.centrexcursionistalcoi.app.database.UsersRepository
import org.centrexcursionistalcoi.app.process.Progress
import org.centrexcursionistalcoi.app.request.UpdateEventRequest
import org.centrexcursionistalcoi.app.storage.InMemoryFileAllocator
import org.centrexcursionistalcoi.app.storage.SETTINGS_LAST_EVENTS_SYNC
import org.centrexcursionistalcoi.app.utils.Zero

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
) {
    override val availableSinceVersionCode: Int = 285

    suspend fun create(
        start: LocalDateTime,
        end: LocalDateTime?,
        place: String,
        title: String,
        description: String,
        maxPeople: String,
        requiresConfirmation: Boolean,
        departmentId: Uuid?,
        image: PlatformFile?,
        progressNotifier: (Progress) -> Unit
    ) {
        val inMemoryImage = image?.let { InMemoryFileAllocator.put(it) }

        create(
            item = Event(
                id = Uuid.Zero,
                start = start.toInstant(TimeZone.currentSystemDefault()),
                end = end?.toInstant(TimeZone.currentSystemDefault()),
                place = place,
                title = title,
                description = description.takeIf { it.isNotBlank() },
                maxPeople = maxPeople.takeIf { it.isNotBlank() }?.toLongOrNull(),
                requiresConfirmation = requiresConfirmation,
                department = departmentId,
                image = inMemoryImage?.id,
                userReferences = emptyList(),
            ),
            progressNotifier,
        )
    }

    suspend fun update(
        id: Uuid,
        start: LocalDateTime?,
        end: LocalDateTime?,
        place: String?,
        title: String?,
        description: String?,
        maxPeople: String?,
        requiresConfirmation: Boolean?,
        departmentId: Uuid?,
        image: PlatformFile?,
        progressNotifier: (Progress) -> Unit
    ) {
        val inMemoryImage = image?.let { InMemoryFileAllocator.put(it) }

        update(
            id,
            request = UpdateEventRequest(
                start = start?.toInstant(TimeZone.currentSystemDefault()),
                end = end?.toInstant(TimeZone.currentSystemDefault()),
                place = place,
                title = title,
                description = description?.takeIf { it.isNotBlank() },
                maxPeople = maxPeople?.takeIf { it.isNotBlank() }?.toLongOrNull(),
                requiresConfirmation = requiresConfirmation,
                department = departmentId,
                image = inMemoryImage?.toFileWithContext(),
            ),
            serializer = UpdateEventRequest.serializer(),
            progressNotifier = progressNotifier,
        )
    }
}
