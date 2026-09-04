package org.centrexcursionistalcoi.app.sync

import org.centrexcursionistalcoi.app.database.EventsRepository
import org.centrexcursionistalcoi.app.network.EventsRemoteRepository
import org.centrexcursionistalcoi.app.utils.toUuidOrNull
import org.koin.core.annotation.Named
import org.koin.core.annotation.Singleton

@Singleton
@Named("SyncEventBackgroundJobLogic")
class SyncEventBackgroundJobLogic(
    private val eventsRemoteRepository: EventsRemoteRepository,
    private val eventsRepository: EventsRepository
) : BackgroundSyncWorkerLogic() {
    override suspend fun BackgroundSyncContext.run(input: Map<String, String>): SyncResult {
        val eventId = input[EXTRA_EVENT_ID]?.toUuidOrNull()
            ?: return SyncResult.Failure("Invalid or missing event ID")

        val event = eventsRemoteRepository.get(eventId, progressNotifier, ignoreIfModifiedSince = true)
            ?: return SyncResult.Failure("Event with ID $eventId not found on server")
        eventsRepository.insertOrUpdate(event)

        return SyncResult.Success()
    }

    companion object {
        const val EXTRA_EVENT_ID = "event_id"
    }
}
