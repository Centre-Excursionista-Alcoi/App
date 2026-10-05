package org.centrexcursionistalcoi.app.network

import com.diamondedge.logging.logging
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.centrexcursionistalcoi.app.GlobalAsyncErrorHandler
import org.centrexcursionistalcoi.app.database.ProfileRepository
import org.centrexcursionistalcoi.app.error.bodyAsError
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.log.TraceOperation
import org.centrexcursionistalcoi.app.log.traceSpan
import org.centrexcursionistalcoi.app.process.Progress
import org.centrexcursionistalcoi.app.process.Progress.Companion.monitorDownloadProgress
import org.centrexcursionistalcoi.app.process.ProgressNotifier
import org.centrexcursionistalcoi.app.response.ProfileResponse
import org.centrexcursionistalcoi.app.routes.Api
import org.centrexcursionistalcoi.app.settings.SettingsStore
import org.centrexcursionistalcoi.app.storage.SETTINGS_LAST_PROFILE_SYNC
import org.koin.core.annotation.Singleton
import org.koin.core.component.KoinComponent

/**
 * Syncs everything with a single request, `GET /sync`, instead of one for each of the repositories: it saves a round
 * trip for each, and the server checking the session over and over.
 *
 * The server answers a section for each entity, with what its own route would answer, and only the ones that changed
 * since the last time they were synced (each repository's own time, see [RemoteRepository.lastSyncForBulkSync]).
 */
@Singleton
class SyncRemoteRepository(
    private val profileRepository: ProfileRepository,
    private val settings: SettingsStore,
    private val departments: DepartmentsRemoteRepository,
    private val users: UsersRemoteRepository,
    private val members: MembersRemoteRepository,
    private val posts: PostsRemoteRepository,
    private val events: EventsRemoteRepository,
    private val inventoryItemTypes: InventoryItemTypesRemoteRepository,
    private val inventoryItems: InventoryItemsRemoteRepository,
    private val lendings: LendingsRemoteRepository,
    private val memories: MemoriesRemoteRepository,
    private val spaces: SpacesRemoteRepository,
    private val spaceKeys: SpaceKeysRemoteRepository,
    private val spaceLendings: SpaceLendingsRemoteRepository,
) : KoinComponent {
    private val log = logging()

    private val httpClient by lazy { getHttpClient() }

    /** A section of the response, and the repository that stores it. */
    private class Section(val key: String, val repository: RemoteRepository<*, *, *, *>)

    /**
     * The sections in the order they have to be stored: each one only refers to the ones before it.
     */
    private val sections: List<Section> by lazy {
        listOf(
            Section("departments", departments),
            Section("users", users),
            Section("members", members),
            Section("posts", posts),
            Section("events", events),
            Section("inventory_types", inventoryItemTypes),
            Section("inventory_items", inventoryItems),
            Section("lendings", lendings),
            Section("memories", memories),
            Section("spaces", spaces),
            Section("space_keys", spaceKeys),
            Section("space_lendings", spaceLendings),
        )
    }

    /**
     * Fetches everything that changed, and stores it.
     * @param ignoreIfModifiedSince Fetch everything, changed or not.
     * @return `false` if the server doesn't have `GET /sync`, so each entity has to be synced on its own.
     * @throws org.centrexcursionistalcoi.app.exception.ServerException if the server answers an error.
     */
    suspend fun synchronize(progress: ProgressNotifier? = null, ignoreIfModifiedSince: Boolean = false): Boolean {
        val response = httpClient.getTraced(Api.Sync()) {
            progress?.let { monitorDownloadProgress(it) }
            // The profile is the only one that is always a row of its own, so it is up to its stored time
            if (!ignoreIfModifiedSince) settings.get(SETTINGS_LAST_PROFILE_SYNC)?.let { parameter(PROFILE_KEY, it) }
            for (section in sections) {
                section.repository.lastSyncForBulkSync(ignoreIfModifiedSince)?.let { parameter(section.key, it) }
            }
        }
        val status = response.status
        if (status == HttpStatusCode.NotFound) {
            log.w { "The server doesn't have /sync, syncing each entity on its own." }
            return false
        } else if (!status.isSuccess()) {
            throw response.bodyAsError().toThrowable().also(GlobalAsyncErrorHandler::setError)
        }

        val body: JsonObject = traceSpan(TraceOperation.SERIALIZE, "Parse /sync") {
            json.parseToJsonElement(response.bodyAsText()).jsonObject
        }
        val serverTime = body.getValue("serverTime").jsonPrimitive.long

        progress?.invoke(Progress.DataProcessing)
        body[PROFILE_KEY]?.jsonObject?.let { section ->
            traceSpan(TraceOperation.SYNC_ENTITY, "profile") { storeProfile(section, serverTime) }
        }
        for (section in sections) {
            val data = body[section.key]?.jsonObject ?: continue
            section.repository.synchronizeFromBulkSync(data, serverTime, progress)
        }
        return true
    }

    private suspend fun storeProfile(section: JsonObject, serverTime: Long) {
        if (section["modified"]?.jsonPrimitive?.content != "true") return
        val profile = json.decodeFromString(ProfileResponse.serializer(), section.getValue("items").toString())
        profileRepository.update(profile)
        settings.set(SETTINGS_LAST_PROFILE_SYNC, serverTime)
    }

    companion object {
        private const val PROFILE_KEY = "profile"
    }
}
