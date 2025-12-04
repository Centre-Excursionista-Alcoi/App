package org.centrexcursionistalcoi.app.sync

import com.diamondedge.logging.logging
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.until
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.network.DepartmentsRemoteRepository
import org.centrexcursionistalcoi.app.network.EventsRemoteRepository
import org.centrexcursionistalcoi.app.network.InventoryItemTypesRemoteRepository
import org.centrexcursionistalcoi.app.network.InventoryItemsRemoteRepository
import org.centrexcursionistalcoi.app.network.LendingsRemoteRepository
import org.centrexcursionistalcoi.app.network.PostsRemoteRepository
import org.centrexcursionistalcoi.app.network.ProfileRemoteRepository
import org.centrexcursionistalcoi.app.network.UsersRemoteRepository
import org.centrexcursionistalcoi.app.process.ProgressNotifier
import org.centrexcursionistalcoi.app.storage.settings

expect class SyncAllDataBackgroundJob : BackgroundSyncWorker<SyncAllDataBackgroundJobLogic>

object SyncAllDataBackgroundJobLogic : BackgroundSyncWorkerLogic() {
    private val log = logging()

    private const val SETTINGS_LAST_SYNC = "lastSync"
    private const val SETTINGS_LAST_SYNC_VERSION = "lastSyncVersion"

    const val EXTRA_FORCE_SYNC = "force_sync"

    /** Run sync every hour */
    const val SYNC_EVERY_SECONDS = 60 * 60

    const val UNIQUE_NAME = "SyncAllDataBackgroundJob"

    /**
     * The interval at which this job should be periodically scheduled.
     */
    val periodicSyncInterval = 4.hours

    /**
     * Checks if the database version has been upgraded since the last sync.
     */
    fun databaseVersionUpgrade(): Boolean {
        val lastSyncVersion = settings.getLongOrNull(SETTINGS_LAST_SYNC_VERSION)?.toInt()
        return lastSyncVersion == null || lastSyncVersion < Database.Schema.version
    }

    override suspend fun BackgroundSyncContext.run(input: Map<String, String>): SyncResult {
        val forceSync = input[EXTRA_FORCE_SYNC]?.toBoolean() ?: false

        val lastSync = settings.getLongOrNull(SETTINGS_LAST_SYNC)?.let { Instant.fromEpochSeconds(it) }
        val now = Clock.System.now()
        return if (
            forceSync ||
            lastSync == null ||
            databaseVersionUpgrade() ||
            lastSync.until(now, DateTimeUnit.SECOND) > SYNC_EVERY_SECONDS
        ) {
            log.d { "Last sync was more than $SYNC_EVERY_SECONDS seconds ago, synchronizing data..." }

            // Synchronize the local database with the remote data
            syncAll(forceSync, progressNotifier)

            SyncResult.Success()
        } else {
            log.d { "Last sync was less than $SYNC_EVERY_SECONDS seconds ago, skipping synchronization." }

            SyncResult.Success()
        }
    }

    suspend fun syncAll(force: Boolean = false, progressNotifier: ProgressNotifier? = null) {
        // First, synchronize the user profile
        ProfileRemoteRepository.synchronize(progressNotifier, ignoreIfModifiedSince = force)

        // Departments does not depend on any other entity, so we sync it first
        DepartmentsRemoteRepository.synchronizeWithDatabase(progressNotifier, ignoreIfModifiedSince = force)
        // Users does not depend on any other entity
        UsersRemoteRepository.synchronizeWithDatabase(progressNotifier, ignoreIfModifiedSince = force)
        // Posts requires Departments
        PostsRemoteRepository.synchronizeWithDatabase(progressNotifier, ignoreIfModifiedSince = force)
        // Events requires Departments and Users
        EventsRemoteRepository.synchronizeWithDatabase(progressNotifier, ignoreIfModifiedSince = force)
        // Inventory Item Types requires Departments
        InventoryItemTypesRemoteRepository.synchronizeWithDatabase(progressNotifier, ignoreIfModifiedSince = force)
        // Inventory Items requires Inventory Item Types
        InventoryItemsRemoteRepository.synchronizeWithDatabase(progressNotifier, ignoreIfModifiedSince = force)
        // Lendings requires Users, Inventory Item Types and Inventory Items
        LendingsRemoteRepository.synchronizeWithDatabase(progressNotifier, ignoreIfModifiedSince = force)

        settings.putLong(SETTINGS_LAST_SYNC, Clock.System.now().epochSeconds)
        settings.putLong(SETTINGS_LAST_SYNC_VERSION, Database.Schema.version)
    }
}
