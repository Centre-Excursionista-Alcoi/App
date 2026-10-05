package org.centrexcursionistalcoi.app.network

import androidx.datastore.preferences.core.Preferences
import com.diamondedge.logging.logging
import io.ktor.client.HttpClient
import io.ktor.client.plugins.onUpload
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.resources.serialization.ResourcesFormat
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonPrimitive
import org.centrexcursionistalcoi.app.GlobalAsyncErrorHandler
import org.centrexcursionistalcoi.app.data.DocumentFileContainer
import org.centrexcursionistalcoi.app.data.Entity
import org.centrexcursionistalcoi.app.data.ImageFileContainer
import org.centrexcursionistalcoi.app.data.fetchDocumentFilePath
import org.centrexcursionistalcoi.app.data.fetchImageFilePath
import org.centrexcursionistalcoi.app.database.Repository
import org.centrexcursionistalcoi.app.error.Error
import org.centrexcursionistalcoi.app.error.bodyAsError
import org.centrexcursionistalcoi.app.exception.MissingCrossReferenceException
import org.centrexcursionistalcoi.app.exception.ResourceNotModifiedException
import org.centrexcursionistalcoi.app.exception.ServerException
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.log.TraceOperation
import org.centrexcursionistalcoi.app.log.TraceSpan
import org.centrexcursionistalcoi.app.log.traceSpan
import org.centrexcursionistalcoi.app.process.Progress
import org.centrexcursionistalcoi.app.process.Progress.Companion.monitorDownloadProgress
import org.centrexcursionistalcoi.app.process.Progress.Companion.monitorUploadProgress
import org.centrexcursionistalcoi.app.process.ProgressNotifier
import org.centrexcursionistalcoi.app.request.UpdateEntityRequest
import org.centrexcursionistalcoi.app.routes.Api
import org.centrexcursionistalcoi.app.routes.EntityResources
import org.centrexcursionistalcoi.app.settings.SettingsStore
import org.centrexcursionistalcoi.app.storage.fs.AppFile
import org.centrexcursionistalcoi.app.storage.fs.write
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import kotlin.time.Clock
import kotlin.uuid.Uuid

private val log = logging()

abstract class RemoteRepository<LocalIdType : Any, LocalEntity : Entity<LocalIdType>, RemoteIdType: Any, RemoteEntity : Entity<RemoteIdType>>(
    private val resources: EntityResources<*, *>,
    private val lastSyncSettingsKey: Preferences.Key<Long>,
    private val serializer: KSerializer<RemoteEntity>,
    private val repository: Repository<LocalEntity, LocalIdType>,
    private val isCreationSupported: Boolean = true,
    private val isPatchSupported: Boolean = true,
    private val remoteToLocalIdConverter: (RemoteIdType) -> LocalIdType,
): KoinComponent {
    /**
     * The version code since which this endpoint is available.
     *
     * If not null, this can be used to check compatibility with the server.
     */
    protected open val availableSinceVersionCode: Int? = null

    private val name = ResourcesFormat().encodeToPathPattern(resources.collectionSerializer).trim('/')

    protected val httpClient = getHttpClient()

    private val settings by lazy { get<SettingsStore>() }
    private val serverInfoRepository by lazy { get<ServerInfoRepository>() }

    // Remove null fields to avoid issues with missing fields in the local model
    private fun String.cleanNullFields() = replace(",? *\"[a-zA-Z0-9_-]+\": *\"?null\"?".toRegex(), "")

    /**
     * Checks if the endpoint is supported by the connected server.
     *
     * Uses [availableSinceVersionCode] and [ServerInfoRepository.info] to determine compatibility.
     * @return `true` if the endpoint is supported, `false` otherwise.
     */
    fun endpointSupported(): Boolean {
        availableSinceVersionCode?.let { availableSince ->
            val info = serverInfoRepository.info
            if (info == null) {
                // versionCode was added on version 2.0.14, so older servers may not have it. The server is considered not compatible.
                log.e { "Could not determine server version. Assuming $name endpoint is not supported." }
                return false
            } else {
                val versionCode = info.version.code
                if (versionCode < availableSince) {
                    log.w { "$name endpoint is not available in version $versionCode (available since $availableSince)" }
                    return false
                }
            }
        }
        return true
    }

    /**
     * Fetches all entities from the remote server.
     * @param progress An optional progress notifier to report progress.
     * @param ignoreIfModifiedSince If `true`, ignores the `If-Modified-Since` header and always fetches data.
     * @return A list of local entities converted from the remote entities.
     * @throws ServerException if the server returns an error response.
     * @throws ResourceNotModifiedException if the data has not changed since the last fetch.
     */
    suspend fun getAll(progress: ProgressNotifier? = null, ignoreIfModifiedSince: Boolean = false): List<RemoteEntity> {
        if (!endpointSupported()) return emptyList()

        val response = httpClient.getTraced {
            collection(httpClient, resources)
            progress?.let { monitorDownloadProgress(it) }
            if (!ignoreIfModifiedSince) ifModifiedSince(lastSyncSettingsKey)
        }
        val status = response.status
        if (status == HttpStatusCode.NotModified) {
            throw ResourceNotModifiedException()
        } else if (status.isSuccess()) {
            val currentTime = Clock.System.now()
            settings.set(lastSyncSettingsKey, currentTime.toEpochMilliseconds())

            val remoteEntities = traceSpan(TraceOperation.SERIALIZE, "Decode $name") {
                val raw = response.bodyAsText().cleanNullFields()
                json.decodeFromString(ListSerializer(serializer), raw)
            }
            return remoteEntities
        } else {
            val error = response.bodyAsError()
            throw error.toThrowable().also(GlobalAsyncErrorHandler::setError)
        }
    }

    /**
     * Fetches an entity from the remote server.
     * @param url Sets the URL of the remote entity to fetch.
     * @param progress An optional progress notifier to report progress.
     * @param ignoreIfModifiedSince If `true`, ignores the `If-Modified-Since` header and always fetches data.
     * @return The local entity converted from the remote entity, or `null` if not found.
     * @throws ResourceNotModifiedException if the data has not changed since the last fetch.
     */
    private suspend fun getUrl(
        progress: ProgressNotifier? = null,
        ignoreIfModifiedSince: Boolean = false,
        url: HttpRequestBuilder.() -> Unit,
    ): RemoteEntity? {
        if (!endpointSupported()) return null

        val response = httpClient.getTraced {
            url()
            progress?.let { monitorDownloadProgress(it) }
            if (!ignoreIfModifiedSince) ifModifiedSince(lastSyncSettingsKey)
        }
        val status = response.status
        if (status == HttpStatusCode.NotModified) {
            throw ResourceNotModifiedException()
        } else if (status.isSuccess()) {
            val currentTime = Clock.System.now()
            settings.set(lastSyncSettingsKey, currentTime.toEpochMilliseconds())

            val raw = response.bodyAsText().cleanNullFields()
            val remoteEntity = json.decodeFromString(serializer, raw)
            return remoteEntity
        } else if (status == HttpStatusCode.NotFound) {
            // A 404 always means "not found" regardless of whether the body could be parsed as
            // an Error.EntityNotFound -- some 404s (e.g. an unmatched route) carry no body at all.
            log.e { "$name #${response.request.url.segments.lastOrNull()} was not found." }
            return null
        } else {
            val error = response.bodyAsError()
            throw error.toThrowable().also(GlobalAsyncErrorHandler::setError)
        }
    }

    /**
     * Fetches the entity with the given ID from the remote server.
     * @param id The ID of the remote entity to fetch.
     * @param progress An optional progress notifier to report progress.
     * @param ignoreIfModifiedSince If `true`, ignores the `If-Modified-Since` header and always fetches data.
     * @return The local entity converted from the remote entity, or `null` if not found.
     * @throws ResourceNotModifiedException if the data has not changed since the last fetch.
     */
    suspend fun get(
        id: RemoteIdType,
        progress: ProgressNotifier? = null,
        ignoreIfModifiedSince: Boolean = false,
    ): RemoteEntity? = getUrl(progress, ignoreIfModifiedSince) { item(httpClient, resources, id.toString()) }

    /**
     * Fetches the entity with the given ID from the remote server and updates or inserts it into the local database.
     * Returns the fetched entity, or `null` if it could not be retrieved.
     *
     * This does not update any associated files; use [synchronizeWithDatabase] for a full sync.
     * @param id The ID of the remote entity to fetch.
     * @param progressNotifier An optional progress notifier to report progress.
     * @param ignoreIfModifiedSince If `true`, ignores the `If-Modified-Since` header and always fetches data.
     * @throws ResourceNotModifiedException if the data has not changed since the last fetch.
     * @return The fetched local entity, or `null` if it could not be retrieved.
     */
    suspend fun update(
        id: RemoteIdType,
        progressNotifier: ProgressNotifier? = null,
        ignoreIfModifiedSince: Boolean = false,
    ): RemoteEntity? {
        val item = get(id, progressNotifier, ignoreIfModifiedSince)
        if (item != null) {
            progressNotifier?.invoke(Progress.LocalDBWrite)
            upsertRemoteEntity(item)
        }
        return item
    }

    /**
     * Brings the local database to what the server has: inserts and updates [remoteList], and deletes what isn't in it.
     * @param localList What the local database has.
     */
    private suspend fun storeRemoteList(
        localList: List<LocalEntity>,
        remoteList: List<RemoteEntity>,
        progress: ProgressNotifier?,
        span: TraceSpan?,
    ) {
        progress?.invoke(Progress.DataProcessing)
        val (toInsert, toUpdate, toDelete) = traceSpan(TraceOperation.SYNC_PROCESS, "Compare $name") {
            val toUpdate = mutableListOf<RemoteEntity>()
            val toInsert = mutableListOf<RemoteEntity>()
            for (item in remoteList) {
                if (localList.find { it.id == item.id } != null) {
                    toUpdate += item
                } else {
                    toInsert += item
                }
            }
            // IDs of items that should remain in the database
            val existingIds = toUpdate.map { remoteToLocalIdConverter(it.id) } + toInsert.map { remoteToLocalIdConverter(it.id) }

            // Delete items that are not in the server response
            val toDelete = localList.filter { it.id !in existingIds }.map { it.id }

            Triple(toInsert, toUpdate, toDelete)
        }
        span?.setData("sync.inserted", toInsert.size.toLong())
        span?.setData("sync.updated", toUpdate.size.toLong())
        span?.setData("sync.deleted", toDelete.size.toLong())

        log.d {
            "Inserting ${toInsert.size} new $name. Updating ${toUpdate.size} $name. Deleting ${toDelete.size} $name"
        }

        progress?.invoke(Progress.LocalDBWrite)
        traceSpan(TraceOperation.DB_WRITE, "Store $name") {
            // Insert new items, and update existing ones: both as upserts, since another sync (e.g. of a single
            // entity, after a push notification) may store the same rows meanwhile
            toInsert.forEach { upsertRemoteEntity(it) }
            toUpdate.forEach { upsertRemoteEntity(it) }
            // Delete removed items
            repository.deleteByIdList(toDelete)
        }

        progress?.invoke(Progress.LocalDBRead)
        val all = repository.selectAll()
        log.i { "There are ${all.size} $name" }
    }

    /**
     * The time this repository was last synced, to send in `GET /sync`, or `null` if its section has to be sent in
     * full: [force]d, or when there's nothing stored locally (the stored time can't be trusted then, see
     * [synchronizeWithDatabase]).
     */
    suspend fun lastSyncForBulkSync(force: Boolean): Long? {
        if (force || repository.selectAll().isEmpty()) return null
        return settings.get(lastSyncSettingsKey)
    }

    /**
     * Stores a section of the response of `GET /sync`: what [getAll] would have fetched.
     * @param section `{"modified":false}`, or `{"modified":true,"items":[...]}`.
     * @param serverTime When the server answered: the next sync is only for what changed after.
     */
    suspend fun synchronizeFromBulkSync(
        section: JsonObject,
        serverTime: Long,
        progress: ProgressNotifier? = null,
    ) = traceSpan(TraceOperation.SYNC_ENTITY, name) { span ->
        if (section["modified"]?.jsonPrimitive?.boolean != true) {
            span?.setData("sync.not_modified", true)
            log.i { "$name not modified. No need to refresh." }
            return@traceSpan
        }
        progress?.invoke(Progress.LocalDBRead)
        val localList = traceSpan(TraceOperation.DB_READ, "Select all $name") { repository.selectAll() }
        val remoteList = traceSpan(TraceOperation.SERIALIZE, "Decode $name") {
            val raw = section.getValue("items").toString().cleanNullFields()
            json.decodeFromString(ListSerializer(serializer), raw)
        }
        storeRemoteList(localList, remoteList, progress, span)
        settings.set(lastSyncSettingsKey, serverTime)
    }

    /**
     * Synchronizes the local database with the remote server.
     * @param progress An optional progress notifier to report progress.
     * @param ignoreIfModifiedSince If `true`, ignores the `If-Modified-Since` header and always fetches data.
     * @throws ServerException if the server returns an error response.
     * @throws MissingCrossReferenceException if a reference of any item is not found.
     */
    suspend fun synchronizeWithDatabase(
        progress: ProgressNotifier? = null,
        ignoreIfModifiedSince: Boolean = false,
    ) = traceSpan(TraceOperation.SYNC_ENTITY, name) { span ->
        try {
            progress?.invoke(Progress.LocalDBRead)
            val localList = traceSpan(TraceOperation.DB_READ, "Select all $name") {
                repository.selectAll() // all entries from the local database
            }

            // A stored "last synced" timestamp cannot be trusted when the local table is empty: the server may
            // legitimately report "not modified" even though there's nothing to show locally (e.g. after the local
            // database was reset without also clearing the per-repository sync timestamps). Force a full fetch then.
            val forceFetch = ignoreIfModifiedSince || localList.isEmpty()
            span?.setData("sync.forced", forceFetch)
            val remoteList = getAll(progress, forceFetch) // all entries from the remote server

            storeRemoteList(localList, remoteList, progress, span)
        } catch (_: ResourceNotModifiedException) {
            span?.setData("sync.not_modified", true)
            log.i { "Resource not modified. No need to refresh." }
        }
    }

    /**
     * Downloads a file with the given UUID from the remote server and saves it to the specified file.
     * @param uuid The UUID of the file to download.
     * @param file The local file where the downloaded file will be saved.
     * @param progressNotifier An optional progress notifier to report download progress.
     */
    suspend fun downloadFile(
        uuid: Uuid,
        file: AppFile,
        progressNotifier: ProgressNotifier? = null
    ) {
        downloadFile(uuid, file, httpClient, progressNotifier)
    }

    private suspend fun downloadFileForEntity(item: LocalEntity, progressNotifier: ProgressNotifier? = null) {
        when (item) {
            is DocumentFileContainer -> {
                val fileUuid = item.documentFile
                if (fileUuid != null) {
                    val file = item.fetchDocumentFilePath(downloadIfNotExists = false)
                    downloadFile(fileUuid, file, progressNotifier)
                } else {
                    log.w { "No document file UUID found for created ${item::class.simpleName}#${item.id}" }
                }
            }
            is ImageFileContainer -> {
                val fileUuid = item.image
                if (fileUuid != null) {
                    val file = item.fetchImageFilePath(downloadIfNotExists = false)
                    downloadFile(fileUuid, file, progressNotifier)
                } else {
                    log.w { "No document file UUID found for created ${item::class.simpleName}#${item.id}" }
                }
            }
            else -> { /* nothing */ }
        }
    }

    /**
     * Handles the response to a `POST` on the collection that just created a new entity.
     */
    private suspend fun handleCreateResponse(response: HttpResponse, progressNotifier: ProgressNotifier?) {
        if (response.status.isSuccess()) {
            try {
                val location = response.headers[HttpHeaders.Location]
                checkNotNull(location) { "Creation didn't return any location for the new item." }

                val item = getUrl(progressNotifier, ignoreIfModifiedSince = true) { url(location) }
                checkNotNull(item) { "Could not retrieve the created item from the server." }
                progressNotifier?.invoke(Progress.LocalDBWrite)
                insertRemoteEntity(item)?.let { downloadFileForEntity(it, progressNotifier) }
            } catch (e: IllegalStateException) {
                log.e { "${e.message} Synchronizing completely with server..." }
                synchronizeWithDatabase(progressNotifier)
            }
        } else {
            // Try to decode the error
            val error = response.bodyAsError()
            log.e { "Failed to create $name: $error" }
            throw error.toThrowable().also(GlobalAsyncErrorHandler::setError)
        }
    }

    /**
     * Creates a new entity from a JSON [request].
     */
    suspend fun <CR : Any> createJson(request: CR, serializer: KSerializer<CR>, progressNotifier: ProgressNotifier? = null) {
        check(isCreationSupported) { "Creation of this entity is not supported" }
        check(endpointSupported()) { "Endpoint $name is not supported on this version." }

        val response = httpClient.post {
            collection(httpClient, resources)
            setBody(requestBody(request, serializer))
            progressNotifier?.let { monitorUploadProgress(it) }
        }
        handleCreateResponse(response, progressNotifier)
    }

    suspend fun <UER : UpdateEntityRequest<RemoteIdType, RemoteEntity>> update(
        id: RemoteIdType,
        request: UER,
        serializer: KSerializer<UER>,
        progressNotifier: ProgressNotifier? = null,
    ) {
        check(isPatchSupported) { "Patching this entity type is not supported" }
        check(endpointSupported()) { "Endpoint $name is not supported on this version." }

        log.d { "Patching $name#$id: $request" }
        val response = httpClient.patch {
            item(httpClient, resources, id.toString())
            setBody(requestBody(request, serializer))
            progressNotifier?.let { notify ->
                onUpload { current, total -> notify(Progress.NamedUpload(id.toString(), current, total)) }
            }
        }
        if (response.status.isSuccess()) {
            val item = if (response.status == HttpStatusCode.NoContent) {
                get(id, progressNotifier, ignoreIfModifiedSince = true)
            } else {
                val location = response.headers[HttpHeaders.Location]
                checkNotNull(location) { "Patch didn't return any location for the new item." }

                getUrl(ignoreIfModifiedSince = true) { url(location) }
            }

            checkNotNull(item) { "Could not retrieve the patched item from the server." }
            progressNotifier?.invoke(Progress.LocalDBWrite)
            updateRemoteEntity(item)?.let { downloadFileForEntity(it, progressNotifier) }
        } else {
            val error = response.bodyAsError()
            log.e { "Failed to update $name#$id: $error" }
            if (error is Error.MalformedRequest) {
                log.e { "Request was malformed: ${json.encodeToString(serializer, request)}" }
            }
            throw error.toThrowable().also(GlobalAsyncErrorHandler::setError)
        }
    }

    suspend fun delete(id: RemoteIdType, progressNotifier: ProgressNotifier? = null) = delete(
        id = id,
        data = null,
        serializer = null,
        progressNotifier = progressNotifier
    )

    suspend fun <T: Any> delete(id: RemoteIdType, data: T?, serializer: KSerializer<T>?, progressNotifier: ProgressNotifier? = null) {
        if (!endpointSupported()) return

        val response = httpClient.delete {
            item(httpClient, resources, id.toString())
            if (data != null && serializer != null) {
                contentType(ContentType.Application.Json)
                val body = json.encodeToString(serializer, data)
                setBody(body)
            }
        }
        if (response.status.isSuccess()) {
            log.i { "Deleted $name with ID $id" }
            progressNotifier?.invoke(Progress.LocalDBWrite)
            repository.delete(remoteToLocalIdConverter(id))
        } else {
            val error = response.bodyAsError()
            log.e { "Failed to delete $name#$id: $error" }
            throw error.toThrowable().also(GlobalAsyncErrorHandler::setError)
        }
    }

    /**
     * Updates the given remote entity in the local database.
     * @param entity The remote entity to update.
     * @return The updated local entity, or `null` if it was stored but can't be read back yet (see [insertRemoteEntity]).
     */
    protected abstract suspend fun updateRemoteEntity(entity: RemoteEntity): LocalEntity?

    /**
     * Inserts the given remote entity into the local database.
     * @param entity The remote entity to insert.
     * @return The inserted local entity, or `null` if it was stored but can't be read back yet: a reference it
     * needs (e.g. a memory's submitter, not visible to non-admins) isn't stored locally, which the
     * [DatabaseIntegrityVerifier][org.centrexcursionistalcoi.app.sync.DatabaseIntegrityVerifier] repairs later.
     */
    protected abstract suspend fun insertRemoteEntity(entity: RemoteEntity): LocalEntity?

    /**
     * Updates or inserts the given remote entity into the local database.
     * @param entity The remote entity to upsert.
     * @return The upserted local entity, or `null` if it was stored but can't be read back yet (see [insertRemoteEntity]).
     */
    protected abstract suspend fun upsertRemoteEntity(entity: RemoteEntity): LocalEntity?


    companion object {
        /**
         * Downloads a file with the given UUID from the remote server and saves it to the specified file.
         * @param uuid The UUID of the file to download.
         * @param file The local file where the downloaded file will be saved.
         * @param httpClient The HTTP client to use for the download. Defaults to the shared client.
         * @param progressNotifier An optional progress notifier to report download progress.
         */
        suspend fun downloadFile(
            uuid: Uuid,
            file: AppFile,
            httpClient: HttpClient = getHttpClient(),
            progressNotifier: ProgressNotifier? = null
        ) {
            log.d { "Downloading $uuid..." }
            val channel = httpClient.getTraced(Api.Download.Id(uuid.toString())) {
                progressNotifier?.let { monitorDownloadProgress(it, uuid.toString()) }
            }.let {
                if (!it.status.isSuccess()) {
                    val error = it.bodyAsError()
                    log.w { "Failed to download file with ID $uuid: $error" }
                    // Not reported here: an image that can't be loaded is shown as such, and other callers report it
                    throw error.toThrowable()
                }
                it.bodyAsChannel()
            }
            log.v { "Writing file..." }
            file.write(channel, progressNotifier)
            log.d { "File $uuid stored." }
        }
    }
}
