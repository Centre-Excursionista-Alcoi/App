package org.centrexcursionistalcoi.app.sync

import org.centrexcursionistalcoi.app.network.EventsRemoteRepository
import org.centrexcursionistalcoi.app.utils.toUuidOrNull
import org.koin.core.annotation.Named
import org.koin.core.annotation.Singleton

@Singleton
@Named("SyncEventBackgroundJob")
class SyncEventBackgroundJob(
    private val eventsRemoteRepository: EventsRemoteRepository,
) : BackgroundJob() {
    override suspend fun BackgroundSyncContext.run(input: Map<String, String>): SyncResult {
        val eventId = input[EXTRA_EVENT_ID]?.toUuidOrNull()
            ?: return SyncResult.Failure("Invalid or missing event ID")

        eventsRemoteRepository.update(eventId, progressNotifier, ignoreIfModifiedSince = true)
            ?: return SyncResult.Failure("Event with ID $eventId not found on server")

        return SyncResult.Success()
    }

    companion object {
        const val EXTRA_EVENT_ID = "event_id"
    }
}
