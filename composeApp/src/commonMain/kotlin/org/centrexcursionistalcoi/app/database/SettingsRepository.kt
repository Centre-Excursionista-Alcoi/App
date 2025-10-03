package org.centrexcursionistalcoi.app.database

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.coroutines.getStringOrNullFlow
import com.russhwolf.settings.observable.makeObservable
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.centrexcursionistalcoi.app.data.Entity
import org.centrexcursionistalcoi.app.storage.settings
import org.centrexcursionistalcoi.app.json as defaultJson

/**
 * The current implementation of SQLite for WASM is broken, and doesn't provide data.
 *
 * This is a workaround that stores the data inside of [settings] instead of a database.
 */
@OptIn(ExperimentalSettingsApi::class)
abstract class SettingsRepository<T : Entity<IdType>, IdType : Any>(
    private val namespace: String,
    private val serializer: KSerializer<T>,
    private val json: Json = defaultJson
) : Repository<T, IdType> {
    override suspend fun selectAll(): List<T> {
        return settings.keys
            .filter { it.startsWith("$namespace.") }
            .mapNotNull { key ->
                settings.getStringOrNull(key)?.let { json.decodeFromString(serializer, it) }
            }
    }

    override fun selectAllAsFlow(dispatcher: CoroutineDispatcher): Flow<List<T>> {
        val settings = settings.makeObservable()
        val flows = settings.keys
            .filter { it.startsWith("$namespace.") }
            .map { settings.getStringOrNullFlow(it) }
        return combine(flows) { values ->
            values
                .filterNotNull()
                .filter { it.isNotEmpty() }
                .mapNotNull {
                    try {
                        json.decodeFromString(serializer, it)
                    } catch (e: SerializationException) {
                        Napier.e("Could not parse item from settings: $it", e)
                        null
                    }
                }
        }
    }

    override suspend fun insert(item: T): Long {
        val data = json.encodeToString(serializer, item)
        settings.putString("${namespace}.${item.id}", data)
        return 1L
    }

    override suspend fun insert(items: List<T>) {
        for (item in items) {
            insert(item)
        }
    }

    override suspend fun update(item: T): Long {
        val data = json.encodeToString(serializer, item)
        settings.putString("${namespace}.${item.id}", data)
        return 1L
    }

    override suspend fun update(items: List<T>) {
        for (item in items) {
            update(item)
        }
    }

    override suspend fun delete(id: IdType) {
        settings.remove("${namespace}.$id")
    }

    override suspend fun deleteByIdList(ids: List<IdType>) {
        for (id in ids) {
            delete(id)
        }
    }
}
