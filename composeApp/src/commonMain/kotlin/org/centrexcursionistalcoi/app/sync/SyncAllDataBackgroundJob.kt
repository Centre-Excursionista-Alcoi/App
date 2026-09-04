package org.centrexcursionistalcoi.app.sync

import cea_app.composeapp.generated.resources.Res
import cea_app.composeapp.generated.resources.sync_step_departments
import cea_app.composeapp.generated.resources.sync_step_events
import cea_app.composeapp.generated.resources.sync_step_item_types
import cea_app.composeapp.generated.resources.sync_step_items
import cea_app.composeapp.generated.resources.sync_step_lendings
import cea_app.composeapp.generated.resources.sync_step_members
import cea_app.composeapp.generated.resources.sync_step_memories
import cea_app.composeapp.generated.resources.sync_step_posts
import cea_app.composeapp.generated.resources.sync_step_profile
import cea_app.composeapp.generated.resources.sync_step_users
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
import org.centrexcursionistalcoi.app.storage.fs.FileSystem
import org.centrexcursionistalcoi.app.storage.settings
import org.koin.core.annotation.Named
import org.koin.core.annotation.Singleton
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

@Singleton
@Named("SyncAllDataBackgroundJob")
class SyncAllDataBackgroundJob(
    private val departmentsRemoteRepository: DepartmentsRemoteRepository,
    private val usersRemoteRepository: UsersRemoteRepository,
    private val membersRemoteRepository: MembersRemoteRepository,
    private val postsRemoteRepository: PostsRemoteRepository,
    private val eventsRemoteRepository: EventsRemoteRepository,
    private val inventoryItemTypesRemoteRepository: InventoryItemTypesRemoteRepository,
    private val inventoryItemsRemoteRepository: InventoryItemsRemoteRepository,
    private val lendingsRemoteRepository: LendingsRemoteRepository,
    private val memoriesRemoteRepository: MemoriesRemoteRepository,

    private val departmentsRepository: DepartmentsRepository,
    private val usersRepository: UsersRepository,
    private val membersRepository: MembersRepository,
    private val postsRepository: PostsRepository,
    private val eventsRepository: EventsRepository,
    private val inventoryItemTypesRepository: InventoryItemTypesRepository,
    private val inventoryItemsRepository: InventoryItemsRepository,
    private val lendingsRepository: LendingsRepository,
    private val memoriesRepository: MemoriesRepository,

    private val authBackend: AuthBackend,
) : BackgroundJob() {
    private val log = logging()

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
            synchronizeAllRepositories(forceSync)

            settings.putLong(SETTINGS_LAST_SYNC, Clock.System.now().epochSeconds)
            settings.putInt(SETTINGS_LAST_SYNC_VERSION, DATABASE_VERSION)

            SyncResult.Success()
        } else {
            log.d { "Last sync was less than $SYNC_EVERY_SECONDS seconds ago, skipping synchronization." }

            SyncResult.Success()
        }
    }

    private suspend fun BackgroundSyncContext.synchronizeAllRepositories(
        force: Boolean,
        isRetry: Boolean = false,
    ) {
        try {
            // First, synchronize the user profile
            ProfileRemoteRepository.synchronize(progressNotifier.withContext(Res.string.sync_step_profile), ignoreIfModifiedSince = force)

            // Departments does not depend on any other entity, so we sync it first
            departmentsRemoteRepository.synchronizeWithDatabase(progressNotifier.withContext(Res.string.sync_step_departments), ignoreIfModifiedSince = force)

            // Users does not depend on any other entity
            usersRemoteRepository.synchronizeWithDatabase(progressNotifier.withContext(Res.string.sync_step_users), ignoreIfModifiedSince = force)

            // Members do not depend on any other entity
            membersRemoteRepository.synchronizeWithDatabase(progressNotifier.withContext(Res.string.sync_step_members), ignoreIfModifiedSince = force)

            // Posts requires Departments
            postsRemoteRepository.synchronizeWithDatabase(progressNotifier.withContext(Res.string.sync_step_posts), ignoreIfModifiedSince = force)

            // Events requires Departments and Users
            // Since users can only be listed by admins, assistance will not be valid for non-admins, StubUser will be filled on all cases
            eventsRemoteRepository.synchronizeWithDatabase(progressNotifier.withContext(Res.string.sync_step_events), ignoreIfModifiedSince = force)

            // Inventory Item Types requires Departments
            inventoryItemTypesRemoteRepository.synchronizeWithDatabase(progressNotifier.withContext(Res.string.sync_step_item_types), ignoreIfModifiedSince = force)

            // Inventory Items requires Inventory Item Types
            inventoryItemsRemoteRepository.synchronizeWithDatabase(progressNotifier.withContext(Res.string.sync_step_items), ignoreIfModifiedSince = force)

            // Lendings requires Users, Inventory Item Types and Inventory Items
            // Since the users list will be filtered for non-admins (only include themselves, and the members of departments they manage, if any),
            // lending user info will not be valid for non-admins, StubUser will be filled on those cases
            lendingsRemoteRepository.synchronizeWithDatabase(progressNotifier.withContext(Res.string.sync_step_lendings), ignoreIfModifiedSince = force)

            // Memories requires Departments and (optionally) Lendings
            memoriesRemoteRepository.synchronizeWithDatabase(progressNotifier.withContext(Res.string.sync_step_memories), ignoreIfModifiedSince = force)
        } catch (e: MissingCrossReferenceException) {
            if (isRetry) {
                log.e(e) { "Could not find cross reference after clearing all local data. Something is wrong on the server side. Failing..." }
                throw e
            } else {
                log.e(e) { "Could not find cross reference. Deleting all local data, and synchronizing again..." }

                log.d { "Removing all data..." }
                // Order is important due to foreign key constraints: children before their parents (the reverse of
                // the sync order above, since Memories has a FK to Lendings).
                memoriesRepository.deleteAll()
                lendingsRepository.deleteAll()
                inventoryItemsRepository.deleteAll()
                inventoryItemTypesRepository.deleteAll()
                eventsRepository.deleteAll()
                postsRepository.deleteAll()
                membersRepository.deleteAll()
                usersRepository.deleteAll()
                departmentsRepository.deleteAll()

                log.d { "Removing all files..." }
                FileSystem.deleteAll().also { log.v { "$it files were deleted." } }

                log.d { "Running sync again..." }
                synchronizeAllRepositories(true, isRetry = true)
            }
        } catch (e: ServerException) {
            if (e.errorCode == Error.ERROR_NOT_LOGGED_IN) {
                log.w { "Not logged in. Credentials may have expired. Logging out..." }
                authBackend.logout()
            } else {
                log.e(e) { "Server error during synchronization. Failing..." }
                throw e
            }
        }
    }

    companion object {
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
    }
}
