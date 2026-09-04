package org.centrexcursionistalcoi.app.network

import io.github.vinceglb.filekit.PlatformFile
import io.ktor.client.request.post
import io.ktor.http.isSuccess
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.centrexcursionistalcoi.app.data.Event
import org.centrexcursionistalcoi.app.data.ReferencedEvent
import org.centrexcursionistalcoi.app.database.EventsRepository
import org.centrexcursionistalcoi.app.exception.ServerException
import org.centrexcursionistalcoi.app.process.ProgressNotifier
import org.centrexcursionistalcoi.app.request.UpdateEventRequest
import org.centrexcursionistalcoi.app.storage.InMemoryFileAllocator
import org.centrexcursionistalcoi.app.storage.SETTINGS_LAST_EVENTS_SYNC
import org.centrexcursionistalcoi.app.utils.Zero
import org.koin.core.annotation.Singleton
import kotlin.uuid.Uuid

@Singleton
class EventsRemoteRepository(
    eventsRepository: EventsRepository,
) : RemoteRepository<Uuid, ReferencedEvent, Uuid, Event>(
    "/events",
    SETTINGS_LAST_EVENTS_SYNC,
    Event.serializer(),
    eventsRepository,
    remoteToLocalIdConverter = { it },
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
        requiresInsurance: Boolean,
        departmentId: Uuid?,
        image: PlatformFile?,
        progressNotifier: ProgressNotifier
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
                requiresInsurance = requiresInsurance,
                department = departmentId,
                image = inMemoryImage?.id,
                userSubList = emptyList(),
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
        requiresInsurance: Boolean?,
        departmentId: Uuid?,
        image: PlatformFile?,
        progressNotifier: ProgressNotifier
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
                requiresInsurance = requiresInsurance,
                department = departmentId,
                image = inMemoryImage?.toFileWithContext(),
            ),
            serializer = UpdateEventRequest.serializer(),
            progressNotifier = progressNotifier,
        )
    }

    suspend fun confirmAssistance(eventId: Uuid) {
        val response = httpClient.post("/events/$eventId/confirm")
        if (!response.status.isSuccess()) throw ServerException.fromResponse(response)
        update(eventId)
    }

    suspend fun rejectAssistance(eventId: Uuid) {
        val response = httpClient.post("/events/$eventId/reject")
        if (!response.status.isSuccess()) throw ServerException.fromResponse(response)
        update(eventId)
    }
}
