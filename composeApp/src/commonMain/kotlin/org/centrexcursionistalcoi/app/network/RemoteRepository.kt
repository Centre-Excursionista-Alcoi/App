package org.centrexcursionistalcoi.app.network

import io.github.aakira.napier.Napier
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
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import org.centrexcursionistalcoi.app.data.DocumentFileContainer
import org.centrexcursionistalcoi.app.data.Entity
import org.centrexcursionistalcoi.app.data.FileContainer
import org.centrexcursionistalcoi.app.data.ImageFileContainer
import org.centrexcursionistalcoi.app.data.SubReferencedFileContainer
import org.centrexcursionistalcoi.app.data.toFormData
import org.centrexcursionistalcoi.app.data.writeFile
import org.centrexcursionistalcoi.app.data.writeImageFile
import org.centrexcursionistalcoi.app.database.Repository
import org.centrexcursionistalcoi.app.error.Error
import org.centrexcursionistalcoi.app.error.bodyAsError
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.process.Progress
import org.centrexcursionistalcoi.app.process.Progress.Companion.monitorDownloadProgress
import org.centrexcursionistalcoi.app.process.Progress.Companion.monitorUploadProgress
import org.centrexcursionistalcoi.app.process.ProgressNotifier
import org.centrexcursionistalcoi.app.request.UpdateEntityRequest

abstract class RemoteRepository<LocalIdType : Any, LocalEntity : Entity<LocalIdType>, RemoteIdType: Any, RemoteEntity : Entity<RemoteIdType>>(
    val endpoint: String,
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

    suspend fun getAll(progress: ProgressNotifier? = null): List<LocalEntity> {
        val remoteEntity = httpClient.get(endpoint) {
            progress?.let { monitorDownloadProgress(it) }
        }.let {
            if (it.status.isSuccess()) {
                val raw = it.bodyAsText().cleanNullFields()
                json.decodeFromString(ListSerializer(serializer), raw)
            } else {
                throw it.bodyAsError().toThrowable()
            }
        }
        return remoteEntity.map { remoteToLocalEntityConverter(it) }
    }

    private suspend fun getUrl(url: String, progress: ProgressNotifier? = null): LocalEntity? {
        val response = httpClient.get(url) {
            progress?.let { monitorDownloadProgress(it) }
        }
        if (response.status.isSuccess()) {
            val raw = response.bodyAsText().cleanNullFields()
            val remoteEntity = json.decodeFromString(serializer, raw)
            return remoteToLocalEntityConverter(remoteEntity)
        } else {
            Napier.e { "Failed to get $name with ID ${url.substringAfterLast('/')}. Status: ${response.status}" }
            return null
        }
    }

    suspend fun get(id: RemoteIdType, progress: ProgressNotifier? = null): LocalEntity? = getUrl("$endpoint/$id", progress)

    /**
     * Fetches the entity with the given ID from the remote server and updates or inserts it into the local database.
     * Returns the fetched entity, or `null` if it could not be retrieved.
     *
     * This does not update any associated files; use [synchronizeWithDatabase] for a full sync.
     * @param id The ID of the remote entity to fetch.
     * @param progressNotifier An optional progress notifier to report progress.
     * @return The fetched local entity, or `null` if it could not be retrieved.
     */
    suspend fun update(id: RemoteIdType, progressNotifier: ProgressNotifier? = null): LocalEntity? {
        val item = get(id, progressNotifier)
        if (item != null) {
            progressNotifier?.invoke(Progress.LocalDBWrite)
            repository.insertOrUpdate(item)
        }
        return item
    }

    suspend fun synchronizeWithDatabase(progress: ProgressNotifier? = null) {
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

        Napier.d {
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
        Napier.i { "There are ${all.size} $name" }

        synchronizeFiles(all, progress)
    }

    private suspend fun synchronizeFiles(entities: List<LocalEntity>, progressNotifier: ProgressNotifier? = null) {
        Napier.d { "Synchronizing files for ${entities.size} entities..." }
        for (item in entities) {
            val itemName = "${item::class.simpleName}#${item.id}"
            if (item is FileContainer) {
                Napier.d { "$itemName has ${item.files.size} files." }
                val files = item.files.toList()
                for ((index, file) in files.withIndex()) {
                    val (fileName, uuid) = file
                    if (uuid == null) {
                        Napier.w { "$itemName is not set for $fileName" }
                        continue
                    }
                    Napier.d { "Downloading $uuid ($index / ${files.size})..." }
                    val channel = httpClient.get("/download/$uuid") {
                        progressNotifier?.let { monitorDownloadProgress(it, uuid.toString()) }
                    }.let {
                        if (!it.status.isSuccess()) {
                            Napier.e { "Failed to download file with ID $uuid for $itemName with ID ${item.id}. Status: ${it.status}" }
                            continue
                        }
                        it.bodyAsChannel()
                    }

                    Napier.v { "Writing file..." }
                    when (item) {
                        is ImageFileContainer -> item.writeImageFile(uuid, channel, progressNotifier)
                        is DocumentFileContainer -> item.writeFile(channel, progressNotifier)
                        else -> item.writeFile(channel, uuid, progressNotifier)
                    }
                    Napier.d { "File $uuid stored." }
                }
            }
            if (item is SubReferencedFileContainer) {
                val files = item.referencedFiles
                Napier.d { "$itemName has ${files.size} referenced files." }
                for ((index, file) in files.withIndex()) {
                    val (fileName, uuid) = file
                    if (uuid == null) {
                        Napier.w { "$itemName is not set for $fileName" }
                        continue
                    }

                    Napier.d { "Downloading $uuid ($index / ${files.size})..." }
                    val channel = httpClient.get("/download/$uuid") {
                        progressNotifier?.let { monitorDownloadProgress(it, uuid.toString()) }
                    }.let {
                        if (!it.status.isSuccess()) {
                            Napier.e { "Failed to download referenced file with ID $uuid for $itemName with ID ${item.id}. Status: ${it.status}" }
                            continue
                        }
                        it.bodyAsChannel()
                    }

                    Napier.v { "Writing referenced file..." }
                    item.writeFile(channel, uuid, progressNotifier)
                    Napier.d { "Referenced file $uuid stored." }
                }
            }
        }
    }

    suspend fun create(item: RemoteEntity, progressNotifier: ProgressNotifier? = null) {
        check(isCreationSupported) { "Creation of this entity is not supported" }

        val response = httpClient.submitFormWithBinaryData(
            url = endpoint,
            formData = item.toFormData()
        ) {
            progressNotifier?.let { monitorUploadProgress(it) }
        }
        if (response.status.isSuccess()) {
            try {
                val location = response.headers[HttpHeaders.Location]
                checkNotNull(location) { "Creation didn't return any location for the new item." }

                val item = getUrl(location, progressNotifier)
                checkNotNull(item) { "Could not retrieve the created item from the server." }
                progressNotifier?.invoke(Progress.LocalDBWrite)
                repository.insert(item)

                synchronizeFiles(listOf(item), progressNotifier)
            } catch (e: IllegalStateException) {
                Napier.e { "${e.message} Synchronizing completely with server..." }
                synchronizeWithDatabase(progressNotifier)
            }
        } else {
            // Try to decode the error
            try {
                val body = response.bodyAsText()
                val error = json.decodeFromString(Error.serializer(), body)
                Napier.e { "Failed to create $name: $error" }
                throw error.toThrowable()
            } catch (e: SerializationException) {
                val throwable = IllegalStateException("Failed to create $name. Status: ${response.status}. Body: ${response.bodyAsText()}", e)
                Napier.e(throwable) { "Failed to create $name. Status: ${response.status}" }
                throw throwable
            }
        }
    }

    suspend fun <UER : UpdateEntityRequest<RemoteIdType, RemoteEntity>> update(
        id: RemoteIdType,
        request: UER,
        serializer: KSerializer<UER>,
        progressNotifier: ProgressNotifier? = null,
    ) {
        check(isPatchSupported) { "Patching this entity type is not supported" }

        Napier.d { "Patching $name#$id: $request" }
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

            val item = getUrl(location)
            checkNotNull(item) { "Could not retrieve the patched item from the server." }
            progressNotifier?.invoke(Progress.LocalDBWrite)
            repository.update(item)

            synchronizeFiles(listOf(item))
        } else {
            Napier.e { "Failed to update $name with ID $id. Status: ${response.status}" }
            throw IllegalStateException("Failed to update $name with ID $id. Status: ${response.status}. Body: ${response.bodyAsText()}")
        }
    }

    suspend fun delete(id: RemoteIdType, progressNotifier: ProgressNotifier? = null) {
        val response = httpClient.delete("$endpoint/$id")
        if (response.status == HttpStatusCode.NoContent) {
            Napier.i { "Deleted $name with ID $id" }
            progressNotifier?.invoke(Progress.LocalDBWrite)
            repository.delete(remoteToLocalIdConverter(id))
        } else {
            Napier.e { "Failed to delete $name with ID $id. Status: ${response.status}" }
            throw IllegalStateException("Failed to delete $name with ID $id. Status: ${response.status}. Body: ${response.bodyAsText()}")
        }
    }
}
