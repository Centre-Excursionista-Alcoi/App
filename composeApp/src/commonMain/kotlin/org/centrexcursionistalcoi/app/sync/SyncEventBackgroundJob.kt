package org.centrexcursionistalcoi.app.sync

import org.centrexcursionistalcoi.app.database.EventsRepository
import org.centrexcursionistalcoi.app.network.EventsRemoteRepository
import org.centrexcursionistalcoi.app.utils.toUuidOrNull

expect class SyncEventBackgroundJob : BackgroundSyncWorker<SyncEventBackgroundJobLogic>

object SyncEventBackgroundJobLogic : BackgroundSyncWorkerLogic() {
    const val EXTRA_EVENT_ID = "event_id"

    override suspend fun BackgroundSyncContext.run(input: Map<String, String>): SyncResult {
        val eventId = input[EXTRA_EVENT_ID]?.toUuidOrNull()
            ?: return SyncResult.Failure("Invalid or missing event ID")

        val event = EventsRemoteRepository.get(eventId, progressNotifier)
            ?: return SyncResult.Failure("Event with ID $eventId not found on server")
        EventsRepository.insertOrUpdate(event)

        return SyncResult.Success()
    }
}
