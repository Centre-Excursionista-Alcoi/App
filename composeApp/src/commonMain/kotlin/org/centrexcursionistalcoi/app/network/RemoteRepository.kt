package org.centrexcursionistalcoi.app.network

import io.github.aakira.napier.Napier
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
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

    suspend fun synchronizeWithDatabase() {
        val list = getAll()
        val toUpdate = mutableListOf<T>()
        val toInsert = mutableListOf<T>()
        for (item in list) {
            if (list.find { it.id == item.id } != null) {
                toUpdate += item
            } else {
                toInsert += item
            }
        }
        // IDs of items that should remain in the database
        val existingIds = toUpdate.map { it.id } + toInsert.map { it.id }

        // Delete items that are not in the server response
        val dbList = repository.selectAll()
        val toDelete = dbList.filter { it.id !in existingIds }.map { it.id }

        Napier.d {
            "Inserting ${toInsert.size} new $name. Updating ${toUpdate.size} $name. Deleting ${toDelete.size} $name"
        }

        // Insert new items
        repository.insert(toInsert)
        // Update existing items
        repository.update(toUpdate)
        // Delete removed items
        repository.deleteByIdList(toDelete)
    }

    suspend fun create(item: T) {
        check(isCreationSupported) { "Creation of this entity is not supported" }

        val response = httpClient.submitFormWithBinaryData(
            url = endpoint,
            formData = formData {
                item.toMap().forEach { (key, value) ->
                    when(value) {
                        is String -> append(key, value)
                        is Number -> append(key, value)
                        is Boolean -> append(key, value)
                        else -> Napier.e { "Unsupported type: ${value?.let { it::class.simpleName } ?: "null"}" }
                    }
                }
            }
        )
        val location = response.headers[HttpHeaders.Location]
        Napier.i { "Created: $location" }
    }
}
