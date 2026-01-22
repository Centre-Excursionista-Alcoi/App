package org.centrexcursionistalcoi.app.sync

import com.diamondedge.logging.logging
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.until
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.DepartmentsRepository
import org.centrexcursionistalcoi.app.database.EventsRepository
import org.centrexcursionistalcoi.app.database.InventoryItemTypesRepository
import org.centrexcursionistalcoi.app.database.InventoryItemsRepository
import org.centrexcursionistalcoi.app.database.LendingsRepository
import org.centrexcursionistalcoi.app.database.MembersRepository
import org.centrexcursionistalcoi.app.database.PostsRepository
import org.centrexcursionistalcoi.app.database.UsersRepository
import org.centrexcursionistalcoi.app.exception.MissingCrossReferenceException
import org.centrexcursionistalcoi.app.network.DepartmentsRemoteRepository
import org.centrexcursionistalcoi.app.network.EventsRemoteRepository
import org.centrexcursionistalcoi.app.network.InventoryItemTypesRemoteRepository
import org.centrexcursionistalcoi.app.network.InventoryItemsRemoteRepository
import org.centrexcursionistalcoi.app.network.LendingsRemoteRepository
import org.centrexcursionistalcoi.app.network.MembersRemoteRepository
import org.centrexcursionistalcoi.app.network.PostsRemoteRepository
import org.centrexcursionistalcoi.app.network.ProfileRemoteRepository
import org.centrexcursionistalcoi.app.network.UsersRemoteRepository
import org.centrexcursionistalcoi.app.process.ProgressNotifier
import org.centrexcursionistalcoi.app.storage.fs.FileSystem
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

    private suspend fun synchronizeAllRepositories(
        force: Boolean,
        progressNotifier: ProgressNotifier?,
        isRetry: Boolean = false,
    ) {
        try {
            // First, synchronize the user profile
            ProfileRemoteRepository.synchronize(progressNotifier, ignoreIfModifiedSince = force)

            // Departments does not depend on any other entity, so we sync it first
            DepartmentsRemoteRepository.synchronizeWithDatabase(progressNotifier, ignoreIfModifiedSince = force)

            // Users does not depend on any other entity
            UsersRemoteRepository.synchronizeWithDatabase(progressNotifier, ignoreIfModifiedSince = force)

            // Members do not depend on any other entity
            MembersRemoteRepository.synchronizeWithDatabase(progressNotifier, ignoreIfModifiedSince = force)

            // Posts requires Departments
            PostsRemoteRepository.synchronizeWithDatabase(progressNotifier, ignoreIfModifiedSince = force)

            // Events requires Departments and Users
            // Since users can only be listed by admins, assistance will not be valid for non-admins, StubUser will be filled on all cases
            EventsRemoteRepository.synchronizeWithDatabase(progressNotifier, ignoreIfModifiedSince = force)

            // Inventory Item Types requires Departments
            InventoryItemTypesRemoteRepository.synchronizeWithDatabase(progressNotifier, ignoreIfModifiedSince = force)

            // Inventory Items requires Inventory Item Types
            InventoryItemsRemoteRepository.synchronizeWithDatabase(progressNotifier, ignoreIfModifiedSince = force)

            // Lendings requires Users, Inventory Item Types and Inventory Items
            // Since the users list will be filtered for non-admins (only include themselves, and the members of departments they manage, if any),
            // lending user info will not be valid for non-admins, StubUser will be filled on those cases
            LendingsRemoteRepository.synchronizeWithDatabase(progressNotifier, ignoreIfModifiedSince = force)
        } catch (e: MissingCrossReferenceException) {
            if (isRetry) {
                log.e(e) { "Could not find cross reference after clearing all local data. Something is wrong on the server side. Failing..." }
                throw e
            } else {
                log.e(e) { "Could not find cross reference. Deleting all local data, and synchronizing again..." }

                log.d { "Removing all data..." }
                // order is important due to foreign key constraints. Same as above
                LendingsRepository.deleteAll()
                InventoryItemsRepository.deleteAll()
                InventoryItemTypesRepository.deleteAll()
                EventsRepository.deleteAll()
                PostsRepository.deleteAll()
                MembersRepository.deleteAll()
                UsersRepository.deleteAll()
                DepartmentsRepository.deleteAll()

                log.d { "Removing all files..." }
                FileSystem.deleteAll().also { log.v { "$it files were deleted." } }

                log.d { "Running sync again..." }
                synchronizeAllRepositories(true, progressNotifier, isRetry = true)
            }
        }
    }

    suspend fun syncAll(force: Boolean = false, progressNotifier: ProgressNotifier? = null) {
        synchronizeAllRepositories(force, progressNotifier)

        settings.putLong(SETTINGS_LAST_SYNC, Clock.System.now().epochSeconds)
        settings.putLong(SETTINGS_LAST_SYNC_VERSION, Database.Schema.version)
    }
}
