package org.centrexcursionistalcoi.app.network

import io.github.aakira.napier.Napier
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
import kotlinx.serialization.builtins.ListSerializer
import org.centrexcursionistalcoi.app.data.Entity
import org.centrexcursionistalcoi.app.data.FileContainer
import org.centrexcursionistalcoi.app.data.ImageFileContainer
import org.centrexcursionistalcoi.app.data.toFormData
import org.centrexcursionistalcoi.app.data.writeImageFile
import org.centrexcursionistalcoi.app.database.Repository
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.request.UpdateEntityRequest

abstract class RemoteRepository<IdType : Any, T : Entity<IdType>>(
    val endpoint: String,
    private val serializer: KSerializer<T>,
    private val repository: Repository<T, IdType>,
    private val isCreationSupported: Boolean = true,
    private val isPatchSupported: Boolean = true,
) {
    private val name = endpoint.trim(' ', '/')

    protected val httpClient = getHttpClient()

    // Remove null fields to avoid issues with missing fields in the local model
    private fun String.cleanNullFields() = replace(",? *\"[a-zA-Z0-9_-]+\": *\"?null\"?".toRegex(), "")

    suspend fun getAll(): List<T> {
        return httpClient.get(endpoint).let {
            val raw = it.bodyAsText().cleanNullFields()
            json.decodeFromString(ListSerializer(serializer), raw)
        }
    }

    private suspend fun getUrl(url: String): T? {
        val response = httpClient.get(url)
        if (response.status.isSuccess()) {
            val raw = response.bodyAsText().cleanNullFields()
            return json.decodeFromString(serializer, raw)
        } else {
            Napier.e { "Failed to get $name with ID ${url.substringAfterLast('/')}. Status: ${response.status}" }
            return null
        }
    }

    suspend fun get(id: IdType): T? = getUrl("$endpoint/$id")

    suspend fun synchronizeWithDatabase() {
        val remoteList = getAll() // all entries from the remote server
        val localList = repository.selectAll() // all entries from the local database
        val toUpdate = mutableListOf<T>()
        val toInsert = mutableListOf<T>()
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

        // Insert new items
        repository.insert(toInsert)
        // Update existing items
        repository.update(toUpdate)
        // Delete removed items
        repository.deleteByIdList(toDelete)

        val all = repository.selectAll()
        Napier.i { "There are ${all.size} $name" }

        Napier.d { "Synchronizing ${all.size} files..." }
        for (item in all) {
            if (item is FileContainer) {
                Napier.d { "${item::class.simpleName}#${item.id} has ${item.files.size} files." }
                for (file in item.files) {
                    val uuid = file.value ?: continue
                    Napier.d { "Downloading $uuid..." }
                    val channel = httpClient.get("/download/$uuid").let {
                        if (!it.status.isSuccess()) {
                            Napier.e { "Failed to download file with ID $uuid for $name with ID ${item.id}. Status: ${it.status}" }
                            continue
                        }
                        it.bodyAsChannel()
                    }

                    Napier.v { "Writing file..." }
                    if (item is ImageFileContainer) {
                        item.writeImageFile(uuid, channel)
                    } else {
                        error("Don't know how to store files for ${item::class.simpleName}. Implementation is missing!")
                    }
                    Napier.d { "File $uuid stored." }
                }
            } else {
                Napier.w { "${item::class.simpleName} is not a FileContainer." }
            }
        }
    }

    suspend fun create(item: T) {
        check(isCreationSupported) { "Creation of this entity is not supported" }

        val response = httpClient.submitFormWithBinaryData(
            url = endpoint,
            formData = item.toFormData()
        )
        try {
            val location = response.headers[HttpHeaders.Location]
            checkNotNull(location) { "Creation didn't return any location for the new item." }

            val item = getUrl(location)
            checkNotNull(item) { "Could not retrieve the created item from the server." }
            repository.insert(item)
        } catch (e: IllegalStateException) {
            Napier.e { "${e.message} Synchronizing completely with server..." }
            synchronizeWithDatabase()
        }
    }

    suspend fun <UER: UpdateEntityRequest<IdType, T>> update(id: IdType, request: UER, serializer: KSerializer<UER>) {
        check(isPatchSupported) { "Patching this entity type is not supported" }

        val response = httpClient.patch("$endpoint/$id") {
            contentType(ContentType.Application.Json)
            val body = json.encodeToString(serializer, request)
            setBody(body)
        }
        if (response.status.isSuccess()) {
            val location = response.headers[HttpHeaders.Location]
            checkNotNull(location) { "Patch didn't return any location for the new item." }

            val item = getUrl(location)
            checkNotNull(item) { "Could not retrieve the patched item from the server." }
            repository.update(item)
        } else {
            Napier.e { "Failed to update $name with ID $id. Status: ${response.status}" }
            throw IllegalStateException("Failed to update $name with ID $id. Status: ${response.status}. Body: ${response.bodyAsText()}")
        }
    }

    suspend fun delete(id: IdType) {
        val response = httpClient.delete("$endpoint/$id")
        if (response.status == HttpStatusCode.NoContent) {
            Napier.i { "Deleted $name with ID $id" }
            repository.delete(id)
        } else {
            Napier.e { "Failed to delete $name with ID $id. Status: ${response.status}" }
            throw IllegalStateException("Failed to delete $name with ID $id. Status: ${response.status}. Body: ${response.bodyAsText()}")
        }
    }
}
