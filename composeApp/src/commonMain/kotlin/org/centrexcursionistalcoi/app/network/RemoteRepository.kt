package org.centrexcursionistalcoi.app.network

import io.github.aakira.napier.Napier
import io.ktor.client.request.delete
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import org.centrexcursionistalcoi.app.data.Entity
import org.centrexcursionistalcoi.app.database.Repository
import org.centrexcursionistalcoi.app.json

abstract class RemoteRepository<IdType: Any, T: Entity<IdType>>(
    val endpoint: String,
    private val serializer: KSerializer<T>,
    private val repository: Repository<T, IdType>,
    private val isCreationSupported: Boolean = true
) {
    private val name = endpoint.trim(' ', '/')

    protected val httpClient = getHttpClient()

    suspend fun getAll(): List<T> {
        return httpClient.get(endpoint).let {
            val raw = it.bodyAsText()
            json.decodeFromString(ListSerializer(serializer), raw)
        }
    }

    private suspend fun getUrl(url: String): T? {
        val response = httpClient.get(url)
        if (response.status.isSuccess()) {
            val raw = response.bodyAsText()
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
    }

    suspend fun create(item: T) {
        check(isCreationSupported) { "Creation of this entity is not supported" }

        val response = httpClient.submitFormWithBinaryData(
            url = endpoint,
            formData = formData {
                item.toMap().forEach { (key, value) ->
                    when(value) {
                        null -> { /* ignore null values */ }
                        is String -> append(key, value)
                        is Number -> append(key, value)
                        is Boolean -> append(key, value)
                        else -> Napier.e { "Unsupported type: ${value::class.simpleName ?: "N/A"}" }
                    }
                }
            }
        )
        val location = response.headers[HttpHeaders.Location]
        checkNotNull(location) { "Creation didn't return any location for the new item." }

        val item = getUrl(location)
        checkNotNull(item) { "Could not retrieve the created item from the server." }
        repository.insert(item)
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
