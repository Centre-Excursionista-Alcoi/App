package org.centrexcursionistalcoi.app.sync

import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import cea_app.composeapp.generated.resources.Res
import cea_app.composeapp.generated.resources.sync_step_departments
import cea_app.composeapp.generated.resources.sync_step_events
import cea_app.composeapp.generated.resources.sync_step_item_types
import cea_app.composeapp.generated.resources.sync_step_items
import cea_app.composeapp.generated.resources.sync_step_lendings
import cea_app.composeapp.generated.resources.sync_step_members
import cea_app.composeapp.generated.resources.sync_step_memories
import cea_app.composeapp.generated.resources.sync_step_posts
import cea_app.composeapp.generated.resources.sync_step_space_keys
import cea_app.composeapp.generated.resources.sync_step_space_lendings
import cea_app.composeapp.generated.resources.sync_step_spaces
import cea_app.composeapp.generated.resources.sync_step_profile
import cea_app.composeapp.generated.resources.sync_step_users
import com.diamondedge.logging.logging
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.until
import org.centrexcursionistalcoi.app.database.DATABASE_VERSION
import org.centrexcursionistalcoi.app.database.DepartmentsRepository
import org.centrexcursionistalcoi.app.database.EventsRepository
import org.centrexcursionistalcoi.app.database.InventoryItemTypesRepository
import org.centrexcursionistalcoi.app.database.InventoryItemsRepository
import org.centrexcursionistalcoi.app.database.LendingsRepository
import org.centrexcursionistalcoi.app.database.MembersRepository
import org.centrexcursionistalcoi.app.database.MemoriesRepository
import org.centrexcursionistalcoi.app.database.PostsRepository
import org.centrexcursionistalcoi.app.database.SpaceKeysRepository
import org.centrexcursionistalcoi.app.database.SpaceLendingsRepository
import org.centrexcursionistalcoi.app.database.SpacesRepository
import org.centrexcursionistalcoi.app.database.UsersRepository
import org.centrexcursionistalcoi.app.exception.MissingCrossReferenceException
import org.centrexcursionistalcoi.app.log.TraceOperation
import org.centrexcursionistalcoi.app.log.traceSpan
import org.centrexcursionistalcoi.app.log.traceTransaction
import org.centrexcursionistalcoi.app.network.DepartmentsRemoteRepository
import org.centrexcursionistalcoi.app.network.EventsRemoteRepository
import org.centrexcursionistalcoi.app.network.InventoryItemTypesRemoteRepository
import org.centrexcursionistalcoi.app.network.InventoryItemsRemoteRepository
import org.centrexcursionistalcoi.app.network.LendingsRemoteRepository
import org.centrexcursionistalcoi.app.network.MembersRemoteRepository
import org.centrexcursionistalcoi.app.network.MemoriesRemoteRepository
import org.centrexcursionistalcoi.app.network.PostsRemoteRepository
import org.centrexcursionistalcoi.app.network.SpaceKeysRemoteRepository
import org.centrexcursionistalcoi.app.network.SpaceLendingsRemoteRepository
import org.centrexcursionistalcoi.app.network.SpacesRemoteRepository
import org.centrexcursionistalcoi.app.network.ProfileRemoteRepository
import org.centrexcursionistalcoi.app.network.UsersRemoteRepository
import org.centrexcursionistalcoi.app.settings.SettingsStore
import org.centrexcursionistalcoi.app.storage.fs.FileSystem
import org.koin.core.annotation.Named
import org.koin.core.annotation.Singleton
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

@Singleton
@Named(SyncAllDataBackgroundJob.UNIQUE_NAME)
class SyncAllDataBackgroundJob(
    private val profileRemoteRepository: ProfileRemoteRepository,
    private val departmentsRemoteRepository: DepartmentsRemoteRepository,
    private val usersRemoteRepository: UsersRemoteRepository,
    private val membersRemoteRepository: MembersRemoteRepository,
    private val postsRemoteRepository: PostsRemoteRepository,
    private val eventsRemoteRepository: EventsRemoteRepository,
    private val inventoryItemTypesRemoteRepository: InventoryItemTypesRemoteRepository,
    private val inventoryItemsRemoteRepository: InventoryItemsRemoteRepository,
    private val lendingsRemoteRepository: LendingsRemoteRepository,
    private val memoriesRemoteRepository: MemoriesRemoteRepository,
    private val spacesRemoteRepository: SpacesRemoteRepository,
    private val spaceKeysRemoteRepository: SpaceKeysRemoteRepository,
    private val spaceLendingsRemoteRepository: SpaceLendingsRemoteRepository,

    private val departmentsRepository: DepartmentsRepository,
    private val usersRepository: UsersRepository,
    private val membersRepository: MembersRepository,
    private val postsRepository: PostsRepository,
    private val eventsRepository: EventsRepository,
    private val inventoryItemTypesRepository: InventoryItemTypesRepository,
    private val inventoryItemsRepository: InventoryItemsRepository,
    private val lendingsRepository: LendingsRepository,
    private val memoriesRepository: MemoriesRepository,
    private val spacesRepository: SpacesRepository,
    private val spaceKeysRepository: SpaceKeysRepository,
    private val spaceLendingsRepository: SpaceLendingsRepository,

    private val settings: SettingsStore,
) : BackgroundJob() {
    private val log = logging()

    override suspend fun BackgroundSyncContext.run(input: Map<String, String>): SyncResult {
        val forceSync = input[EXTRA_FORCE_SYNC]?.toBoolean() ?: false
        // A schema version bump may have just added/backfilled columns on rows that survived a real (non-destructive)
        // migration -- see DatabaseMigrations.kt -- with nothing local to show for them yet. The server has no reason
        // to have bumped affected entities' own lastUpdate just because the client's local schema changed, so a plain
        // If-Modified-Since sync could get a 304 and leave those columns null indefinitely. Force a real refetch here.
        val justUpgraded = settings.databaseVersionUpgrade()

        val lastSync = settings.get(SETTINGS_LAST_SYNC)?.let { Instant.fromEpochSeconds(it) }
        val now = Clock.System.now()
        return if (
            forceSync ||
            lastSync == null ||
            justUpgraded ||
            lastSync.until(now, DateTimeUnit.SECOND) > SYNC_EVERY_SECONDS
        ) {
            log.d { "Last sync was more than $SYNC_EVERY_SECONDS seconds ago, synchronizing data..." }

            // Synchronize the local database with the remote data
            traceTransaction("Sync all data", TraceOperation.SYNC) { transaction ->
                transaction.setTag(
                    "sync.reason",
                    when {
                        lastSync == null -> "initial"
                        justUpgraded -> "database_upgrade"
                        forceSync -> "forced"
                        else -> "periodic"
                    }
                )
                synchronizeAllRepositories(forceSync || justUpgraded)
            }

            settings.set(SETTINGS_LAST_SYNC, Clock.System.now().epochSeconds)
            settings.set(SETTINGS_LAST_SYNC_VERSION, DATABASE_VERSION)

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
            traceSpan(TraceOperation.SYNC_ENTITY, "profile") {
                profileRemoteRepository.synchronize(progressNotifier.withContext(Res.string.sync_step_profile), ignoreIfModifiedSince = force)
            }

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

            // Spaces do not depend on any other entity
            spacesRemoteRepository.synchronizeWithDatabase(progressNotifier.withContext(Res.string.sync_step_spaces), ignoreIfModifiedSince = force)

            // Space keys require Spaces
            spaceKeysRemoteRepository.synchronizeWithDatabase(progressNotifier.withContext(Res.string.sync_step_space_keys), ignoreIfModifiedSince = force)

            // Space lendings require Spaces
            spaceLendingsRemoteRepository.synchronizeWithDatabase(progressNotifier.withContext(Res.string.sync_step_space_lendings), ignoreIfModifiedSince = force)
        } catch (e: MissingCrossReferenceException) {
            if (isRetry) {
                log.e(e) { "Could not find cross reference after clearing all local data. Something is wrong on the server side. Failing..." }
                throw e
            } else {
                log.e(e) { "Could not find cross reference. Deleting all local data, and synchronizing again..." }

                log.d { "Removing all data..." }
                // Order is important due to foreign key constraints: children before their parents (the reverse of
                // the sync order above, since Memories has a FK to Lendings).
                spaceLendingsRepository.deleteAll()
                spaceKeysRepository.deleteAll()
                spacesRepository.deleteAll()
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
                traceSpan(TraceOperation.SYNC_RETRY, "Missing cross reference") {
                    synchronizeAllRepositories(true, isRetry = true)
                }
            }
        }
        // ServerException (including a "not logged in" session expiry) is deliberately left to propagate: it's
        // reported once, centrally, via GlobalAsyncErrorHandler (every RemoteRepository failure funnels through
        // it -- see RemoteRepository.kt), which tries AuthBackend.tryAutoRelogin() before falling back to a
        // real logout. Catching and handling "not logged in" here too used to race that global handling: this
        // job would silently swallow it and report SUCCEEDED, so DatabaseIntegrityVerifier.clearDatabaseAndResync()
        // (which awaits this job) treated a session that just gave up as if it had synced cleanly, sometimes
        // navigating to the Main screen with a wiped, empty database while the user was never actually
        // re-authenticated.
    }

    companion object {
        private val SETTINGS_LAST_SYNC = longPreferencesKey("lastSync")
        private val SETTINGS_LAST_SYNC_VERSION = intPreferencesKey("lastSyncDbVersion")

        const val EXTRA_FORCE_SYNC = "force_sync"

        /** Run sync every hour */
        const val SYNC_EVERY_SECONDS = 60 * 60

        /** Doubles as both the WorkManager unique-work name and this job's Koin qualifier (see [Named]). */
        const val UNIQUE_NAME = "SyncAllDataBackgroundJob"

        /**
         * The interval at which this job should be periodically scheduled.
         */
        val periodicSyncInterval = 4.hours

        /**
         * Checks if the database version has been upgraded since the last sync.
         */
        suspend fun SettingsStore.databaseVersionUpgrade(): Boolean {
            val lastSyncVersion = get(SETTINGS_LAST_SYNC_VERSION)
            return lastSyncVersion == null || lastSyncVersion < DATABASE_VERSION
        }
    }
}
