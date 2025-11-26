package org.centrexcursionistalcoi.app.network

import com.diamondedge.logging.logging
import io.ktor.client.HttpClient
import io.ktor.client.plugins.onUpload
import io.ktor.client.request.delete
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlin.time.Clock
import kotlin.uuid.Uuid
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import org.centrexcursionistalcoi.app.GlobalAsyncErrorHandler
import org.centrexcursionistalcoi.app.data.DocumentFileContainer
import org.centrexcursionistalcoi.app.data.Entity
import org.centrexcursionistalcoi.app.data.FileContainer
import org.centrexcursionistalcoi.app.data.ImageFileContainer
import org.centrexcursionistalcoi.app.data.fetchDocumentFilePath
import org.centrexcursionistalcoi.app.data.fetchImageFilePath
import org.centrexcursionistalcoi.app.data.filePaths
import org.centrexcursionistalcoi.app.data.toFormData
import org.centrexcursionistalcoi.app.database.Repository
import org.centrexcursionistalcoi.app.error.Error
import org.centrexcursionistalcoi.app.error.bodyAsError
import org.centrexcursionistalcoi.app.exception.ResourceNotModifiedException
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.process.Progress
import org.centrexcursionistalcoi.app.process.Progress.Companion.monitorDownloadProgress
import org.centrexcursionistalcoi.app.process.Progress.Companion.monitorUploadProgress
import org.centrexcursionistalcoi.app.process.ProgressNotifier
import org.centrexcursionistalcoi.app.request.UpdateEntityRequest
import org.centrexcursionistalcoi.app.storage.fs.FileSystem
import org.centrexcursionistalcoi.app.storage.settings

private val log = logging()

abstract class RemoteRepository<LocalIdType : Any, LocalEntity : Entity<LocalIdType>, RemoteIdType: Any, RemoteEntity : Entity<RemoteIdType>>(
    val endpoint: String,
    private val lastSyncSettingsKey: String,
    private val serializer: KSerializer<RemoteEntity>,
    private val repository: Repository<LocalEntity, LocalIdType>,
    private val isCreationSupported: Boolean = true,
    private val isPatchSupported: Boolean = true,
    private val remoteToLocalIdConverter: (RemoteIdType) -> LocalIdType,
    private val remoteToLocalEntityConverter: suspend (RemoteEntity) -> LocalEntity,
) {
    private val name = endpoint.trim(' ', '/')

    protected val httpClient = getHttpClient()

    // Remove null fields to avoid issues with missing fields in the local model
    private fun String.cleanNullFields() = replace(",? *\"[a-zA-Z0-9_-]+\": *\"?null\"?".toRegex(), "")

    /**
     * Fetches all entities from the remote server.
     * @param progress An optional progress notifier to report progress.
     * @param ignoreIfModifiedSince If `true`, ignores the `If-Modified-Since` header and always fetches data.
     * @return A list of local entities converted from the remote entities.
     * @throws ResourceNotModifiedException if the data has not changed since the last fetch.
     */
    suspend fun getAll(progress: ProgressNotifier? = null, ignoreIfModifiedSince: Boolean = false): List<LocalEntity> {
        val response = httpClient.get(endpoint) {
            progress?.let { monitorDownloadProgress(it) }
            if (!ignoreIfModifiedSince) ifModifiedSince(lastSyncSettingsKey)
        }
        val status = response.status
        if (status == HttpStatusCode.NotModified) {
            throw ResourceNotModifiedException()
        } else if (status.isSuccess()) {
            val currentTime = Clock.System.now()
            settings.putLong(lastSyncSettingsKey, currentTime.toEpochMilliseconds())

            val raw = response.bodyAsText().cleanNullFields()
            val remoteEntity = json.decodeFromString(ListSerializer(serializer), raw)
            return remoteEntity.map { remoteToLocalEntityConverter(it) }
        } else {
            val error = response.bodyAsError()
            throw error.toThrowable().also(GlobalAsyncErrorHandler::setError)
        }
    }

    /**
     * Fetches the entity with the given URL from the remote server.
     * @param url The URL of the remote entity to fetch.
     * @param progress An optional progress notifier to report progress.
     * @param ignoreIfModifiedSince If `true`, ignores the `If-Modified-Since` header and always fetches data.
     * @return The local entity converted from the remote entity, or `null` if not found.
     * @throws ResourceNotModifiedException if the data has not changed since the last fetch.
     */
    private suspend fun getUrl(
        url: String,
        progress: ProgressNotifier? = null,
        ignoreIfModifiedSince: Boolean = false,
    ): LocalEntity? {
        val response = httpClient.get(url) {
            progress?.let { monitorDownloadProgress(it) }
            if (!ignoreIfModifiedSince) ifModifiedSince(lastSyncSettingsKey)
        }
        val status = response.status
        if (status == HttpStatusCode.NotModified) {
            throw ResourceNotModifiedException()
        } else if (status.isSuccess()) {
            val currentTime = Clock.System.now()
            settings.putLong(lastSyncSettingsKey, currentTime.toEpochMilliseconds())

            val raw = response.bodyAsText().cleanNullFields()
            val remoteEntity = json.decodeFromString(serializer, raw)
            return remoteToLocalEntityConverter(remoteEntity)
        } else {
            val error = response.bodyAsError()
            if (error is Error.EntityNotFound) {
                log.e { "$name #${url.substringAfterLast('/')} was not found." }
                return null
            } else {
                throw error.toThrowable().also(GlobalAsyncErrorHandler::setError)
            }
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
    ): LocalEntity? = getUrl("$endpoint/$id", progress, ignoreIfModifiedSince)

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
    ): LocalEntity? {
        val item = get(id, progressNotifier, ignoreIfModifiedSince)
        if (item != null) {
            progressNotifier?.invoke(Progress.LocalDBWrite)
            repository.insertOrUpdate(item)
        }
        return item
    }

    suspend fun synchronizeWithDatabase(progress: ProgressNotifier? = null) {
        try {
            val remoteList = getAll(progress) // all entries from the remote server

            progress?.invoke(Progress.LocalDBRead)
            val localList = repository.selectAll() // all entries from the local database

            progress?.invoke(Progress.DataProcessing)
            val toUpdate = mutableListOf<LocalEntity>()
            val toInsert = mutableListOf<LocalEntity>()
            for (item in remoteList) {
                if (localList.find { it.id == item.id } != null) {
                    toUpdate += item
                } else {
                    toInsert += item
                }
            }
            // IDs of items that should remain in the database
            val existingIds = toUpdate.map { it.id } + toInsert.map { it.id }

            // Delete items that are not in the server response
            val toDelete = localList.filter { it.id !in existingIds }.map { it.id }

            log.d {
                "Inserting ${toInsert.size} new $name. Updating ${toUpdate.size} $name. Deleting ${toDelete.size} $name"
            }

            progress?.invoke(Progress.LocalDBWrite)
            // Insert new items
            repository.insert(toInsert)
            // Update existing items
            repository.update(toUpdate)
            // Delete removed items
            repository.deleteByIdList(toDelete)

            progress?.invoke(Progress.LocalDBRead)
            val all = repository.selectAll()
            log.i { "There are ${all.size} $name" }
        } catch (_: ResourceNotModifiedException) {
            log.i { "Resource not modified. No need to refresh." }
        }
    }

    /**
     * Downloads a file with the given UUID from the remote server and saves it to the specified path.
     * @param uuid The UUID of the file to download.
     * @param path The local file path where the downloaded file will be saved.
     * @param progressNotifier An optional progress notifier to report download progress.
     */
    suspend fun downloadFile(
        uuid: Uuid,
        path: String,
        progressNotifier: ProgressNotifier? = null
    ) {
        downloadFile(uuid, path, httpClient, progressNotifier)
    }

    private suspend fun downloadFileForEntity(item: LocalEntity, progressNotifier: ProgressNotifier? = null) {
        when (item) {
            is DocumentFileContainer -> {
                val path = item.fetchDocumentFilePath(downloadIfNotExists = false)
                val fileUuid = item.documentFile
                if (fileUuid != null) {
                    downloadFile(fileUuid, path, progressNotifier)
                } else {
                    log.w { "No document file UUID found for created ${item::class.simpleName}#${item.id}" }
                }
            }
            is ImageFileContainer -> {
                val path = item.fetchImageFilePath(downloadIfNotExists = false)
                val fileUuid = item.image
                if (fileUuid != null) {
                    downloadFile(fileUuid, path, progressNotifier)
                } else {
                    log.w { "No document file UUID found for created ${item::class.simpleName}#${item.id}" }
                }
            }
            is FileContainer -> {
                val filePaths = item.filePaths()
                for ((fileUuid, path) in filePaths) {
                    downloadFile(fileUuid, path, progressNotifier)
                }
            }
            else -> { /* nothing */ }
        }
    }

    suspend fun create(item: RemoteEntity, progressNotifier: ProgressNotifier? = null) {
        check(isCreationSupported) { "Creation of this entity is not supported" }

        val formData = item.toFormData()
        val response = httpClient.submitFormWithBinaryData(
            url = endpoint,
            formData = formData
        ) {
            progressNotifier?.let { monitorUploadProgress(it) }
        }
        if (response.status.isSuccess()) {
            try {
                val location = response.headers[HttpHeaders.Location]
                checkNotNull(location) { "Creation didn't return any location for the new item." }

                val item = getUrl(location, progressNotifier, ignoreIfModifiedSince = true)
                checkNotNull(item) { "Could not retrieve the created item from the server." }
                progressNotifier?.invoke(Progress.LocalDBWrite)
                repository.insert(item)

                downloadFileForEntity(item, progressNotifier)
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

    suspend fun <UER : UpdateEntityRequest<RemoteIdType, RemoteEntity>> update(
        id: RemoteIdType,
        request: UER,
        serializer: KSerializer<UER>,
        progressNotifier: ProgressNotifier? = null,
    ) {
        check(isPatchSupported) { "Patching this entity type is not supported" }

        log.d { "Patching $name#$id: $request" }
        val response = httpClient.patch("$endpoint/$id") {
            contentType(ContentType.Application.Json)
            val body = json.encodeToString(serializer, request)
            setBody(body)
            progressNotifier?.let { notify ->
                onUpload { current, total -> notify(Progress.NamedUpload(id.toString(), current, total)) }
            }
        }
        if (response.status.isSuccess()) {
            val location = response.headers[HttpHeaders.Location]
            checkNotNull(location) { "Patch didn't return any location for the new item." }

            val item = getUrl(location, ignoreIfModifiedSince = true)
            checkNotNull(item) { "Could not retrieve the patched item from the server." }
            progressNotifier?.invoke(Progress.LocalDBWrite)
            repository.update(item)

            downloadFileForEntity(item, progressNotifier)
        } else {
            val error = response.bodyAsError()
            log.e { "Failed to update $name#$id: $error" }
            if (error is Error.MalformedRequest) {
                log.e { "Request was malformed: ${json.encodeToString(serializer, request)}" }
            }
            throw error.toThrowable().also(GlobalAsyncErrorHandler::setError)
        }
    }

    suspend fun delete(id: RemoteIdType, progressNotifier: ProgressNotifier? = null) {
        val response = httpClient.delete("$endpoint/$id")
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


    companion object {
        /**
         * Downloads a file with the given UUID from the remote server and saves it to the specified path.
         * @param uuid The UUID of the file to download.
         * @param path The local file path where the downloaded file will be saved.
         * @param httpClient The HTTP client to use for the download. Defaults to the shared client.
         * @param progressNotifier An optional progress notifier to report download progress.
         */
        suspend fun downloadFile(
            uuid: Uuid,
            path: String,
            httpClient: HttpClient = getHttpClient(),
            progressNotifier: ProgressNotifier? = null
        ) {
            log.d { "Downloading $uuid..." }
            val channel = httpClient.get("/download/$uuid") {
                progressNotifier?.let { monitorDownloadProgress(it, uuid.toString()) }
            }.let {
                if (!it.status.isSuccess()) {
                    val error = it.bodyAsError()
                    log.e { "Failed to download file with ID $uuid: $error" }
                    throw error.toThrowable().also(GlobalAsyncErrorHandler::setError)
                }
                it.bodyAsChannel()
            }
            log.v { "Writing file..." }
            FileSystem.write(path, channel, progressNotifier)
            log.d { "File $uuid stored." }
        }
    }
}
