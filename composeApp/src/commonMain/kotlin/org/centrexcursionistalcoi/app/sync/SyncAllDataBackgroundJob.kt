package org.centrexcursionistalcoi.app.sync

import com.diamondedge.logging.logging
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.until
import org.centrexcursionistalcoi.app.auth.AuthBackend
import org.centrexcursionistalcoi.app.database.DATABASE_VERSION
import org.centrexcursionistalcoi.app.database.DepartmentsRepository
import org.centrexcursionistalcoi.app.database.EventsRepository
import org.centrexcursionistalcoi.app.database.InventoryItemTypesRepository
import org.centrexcursionistalcoi.app.database.InventoryItemsRepository
import org.centrexcursionistalcoi.app.database.LendingsRepository
import org.centrexcursionistalcoi.app.database.MembersRepository
import org.centrexcursionistalcoi.app.database.MemoriesRepository
import org.centrexcursionistalcoi.app.database.PostsRepository
import org.centrexcursionistalcoi.app.database.UsersRepository
import org.centrexcursionistalcoi.app.error.Error
import org.centrexcursionistalcoi.app.exception.MissingCrossReferenceException
import org.centrexcursionistalcoi.app.exception.ServerException
import org.centrexcursionistalcoi.app.network.DepartmentsRemoteRepository
import org.centrexcursionistalcoi.app.network.EventsRemoteRepository
import org.centrexcursionistalcoi.app.network.InventoryItemTypesRemoteRepository
import org.centrexcursionistalcoi.app.network.InventoryItemsRemoteRepository
import org.centrexcursionistalcoi.app.network.LendingsRemoteRepository
import org.centrexcursionistalcoi.app.network.MembersRemoteRepository
import org.centrexcursionistalcoi.app.network.MemoriesRemoteRepository
import org.centrexcursionistalcoi.app.network.PostsRemoteRepository
import org.centrexcursionistalcoi.app.network.ProfileRemoteRepository
import org.centrexcursionistalcoi.app.network.UsersRemoteRepository
import org.centrexcursionistalcoi.app.process.ProgressNotifier
import org.centrexcursionistalcoi.app.storage.fs.FileSystem
import org.centrexcursionistalcoi.app.storage.settings
import org.koin.core.component.get
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

expect class SyncAllDataBackgroundJob : BackgroundSyncWorker<SyncAllDataBackgroundJobLogic>

object SyncAllDataBackgroundJobLogic : BackgroundSyncWorkerLogic() {
    private val log = logging()

    private const val SETTINGS_LAST_SYNC = "lastSync"
    private const val SETTINGS_LAST_SYNC_VERSION = "lastSyncDbVersion"

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
        val lastSyncVersion = settings.getIntOrNull(SETTINGS_LAST_SYNC_VERSION)
        return lastSyncVersion == null || lastSyncVersion < DATABASE_VERSION
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
            get<DepartmentsRemoteRepository>().synchronizeWithDatabase(progressNotifier, ignoreIfModifiedSince = force)

            // Users does not depend on any other entity
            get<UsersRemoteRepository>().synchronizeWithDatabase(progressNotifier, ignoreIfModifiedSince = force)

            // Members do not depend on any other entity
            get<MembersRemoteRepository>().synchronizeWithDatabase(progressNotifier, ignoreIfModifiedSince = force)

            // Posts requires Departments
            get<PostsRemoteRepository>().synchronizeWithDatabase(progressNotifier, ignoreIfModifiedSince = force)

            // Events requires Departments and Users
            // Since users can only be listed by admins, assistance will not be valid for non-admins, StubUser will be filled on all cases
            get<EventsRemoteRepository>().synchronizeWithDatabase(progressNotifier, ignoreIfModifiedSince = force)

            // Inventory Item Types requires Departments
            get<InventoryItemTypesRemoteRepository>().synchronizeWithDatabase(progressNotifier, ignoreIfModifiedSince = force)

            // Inventory Items requires Inventory Item Types
            get<InventoryItemsRemoteRepository>().synchronizeWithDatabase(progressNotifier, ignoreIfModifiedSince = force)

            // Memories only reference Departments and (optionally) Lendings by id, no resolution is needed. They
            // must be synced before Lendings, since a lending's memory is resolved from what's already cached here.
            get<MemoriesRemoteRepository>().synchronizeWithDatabase(progressNotifier, ignoreIfModifiedSince = force)

            // Lendings requires Users, Inventory Item Types, Inventory Items and Memories
            // Since the users list will be filtered for non-admins (only include themselves, and the members of departments they manage, if any),
            // lending user info will not be valid for non-admins, StubUser will be filled on those cases
            get<LendingsRemoteRepository>().synchronizeWithDatabase(progressNotifier, ignoreIfModifiedSince = force)
        } catch (e: MissingCrossReferenceException) {
            if (isRetry) {
                log.e(e) { "Could not find cross reference after clearing all local data. Something is wrong on the server side. Failing..." }
                throw e
            } else {
                log.e(e) { "Could not find cross reference. Deleting all local data, and synchronizing again..." }

                log.d { "Removing all data..." }
                // order is important due to foreign key constraints. Same as above
                get<MemoriesRepository>().deleteAll()
                get<LendingsRepository>().deleteAll()
                get<InventoryItemsRepository>().deleteAll()
                get<InventoryItemTypesRepository>().deleteAll()
                get<EventsRepository>().deleteAll()
                get<PostsRepository>().deleteAll()
                get<MembersRepository>().deleteAll()
                get<UsersRepository>().deleteAll()
                get<DepartmentsRepository>().deleteAll()

                log.d { "Removing all files..." }
                FileSystem.deleteAll().also { log.v { "$it files were deleted." } }

                log.d { "Running sync again..." }
                synchronizeAllRepositories(true, progressNotifier, isRetry = true)
            }
        } catch (e: ServerException) {
            if (e.errorCode == Error.ERROR_NOT_LOGGED_IN) {
                log.w { "Not logged in. Credentials may have expired. Logging out..." }
                get<AuthBackend>().logout()
            } else {
                log.e(e) { "Server error during synchronization. Failing..." }
                throw e
            }
        }
    }

    suspend fun syncAll(force: Boolean = false, progressNotifier: ProgressNotifier? = null) {
        synchronizeAllRepositories(force, progressNotifier)

        settings.putLong(SETTINGS_LAST_SYNC, Clock.System.now().epochSeconds)
        settings.putInt(SETTINGS_LAST_SYNC_VERSION, DATABASE_VERSION)
    }
}
