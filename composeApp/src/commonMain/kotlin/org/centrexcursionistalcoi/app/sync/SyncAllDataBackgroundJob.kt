package org.centrexcursionistalcoi.app.sync

import io.github.aakira.napier.Napier
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.until
import org.centrexcursionistalcoi.app.network.DepartmentsRemoteRepository
import org.centrexcursionistalcoi.app.network.InventoryItemTypesRemoteRepository
import org.centrexcursionistalcoi.app.network.InventoryItemsRemoteRepository
import org.centrexcursionistalcoi.app.network.LendingsRemoteRepository
import org.centrexcursionistalcoi.app.network.PostsRemoteRepository
import org.centrexcursionistalcoi.app.network.SpacesRemoteRepository
import org.centrexcursionistalcoi.app.network.UsersRemoteRepository
import org.centrexcursionistalcoi.app.storage.settings

expect class SyncAllDataBackgroundJob : BackgroundSyncWorker<SyncAllDataBackgroundJobLogic>

object SyncAllDataBackgroundJobLogic : BackgroundSyncWorkerLogic() {
    const val EXTRA_FORCE_SYNC = "force_sync"

    /** Run sync every hour */
    const val SYNC_EVERY_SECONDS = 60 * 60

    const val UNIQUE_NAME = "SyncAllDataBackgroundJob"

    /**
     * The interval at which this job should be periodically scheduled.
     */
    val periodicSyncInterval = 4.hours

    override suspend fun BackgroundSyncContext.run(input: Map<String, String>): SyncResult {
        val forceSync = input[EXTRA_FORCE_SYNC]?.toBoolean() ?: false

        val lastSync = settings.getLongOrNull("lastSync")?.let { Instant.fromEpochSeconds(it) }
        val now = Clock.System.now()
        return if (forceSync || lastSync == null || lastSync.until(now, DateTimeUnit.SECOND) > SYNC_EVERY_SECONDS) {
            Napier.d { "Last sync was more than $SYNC_EVERY_SECONDS seconds ago, synchronizing data..." }

            // Synchronize the local database with the remote data
            DepartmentsRemoteRepository.synchronizeWithDatabase(progressNotifier)
            PostsRemoteRepository.synchronizeWithDatabase(progressNotifier)
            InventoryItemTypesRemoteRepository.synchronizeWithDatabase(progressNotifier)
            InventoryItemsRemoteRepository.synchronizeWithDatabase(progressNotifier)
            UsersRemoteRepository.synchronizeWithDatabase(progressNotifier)
            LendingsRemoteRepository.synchronizeWithDatabase(progressNotifier)
            SpacesRemoteRepository.synchronizeWithDatabase(progressNotifier)

            settings.putLong("lastSync", now.epochSeconds)

            SyncResult.Success()
        } else {
            Napier.d { "Last sync was less than $SYNC_EVERY_SECONDS seconds ago, skipping synchronization." }

            SyncResult.Success()
        }
    }
}
