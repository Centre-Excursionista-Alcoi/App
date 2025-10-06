package org.centrexcursionistalcoi.app.database

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.coroutines.getStringOrNullFlow
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map
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
    private val _keysFlow = MutableStateFlow(settings.keys)

    private fun decode(raw: String) = try {
        json.decodeFromString(serializer, raw)
    } catch (e: SerializationException) {
        Napier.e("Could not parse item from settings: $raw", e)
        null
    }

    override suspend fun selectAll(): List<T> {
        return settings.keys
            .filter { it.startsWith("$namespace.") }
            .mapNotNull { key ->
                settings.getStringOrNull(key)?.let { decode(it) }
            }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun selectAllAsFlow(dispatcher: CoroutineDispatcher): Flow<List<T>> {
        return _keysFlow
            // Only keys that match the namespace
            .map { keys -> keys.filter { it.startsWith("$namespace.") } }
            .flatMapConcat { keys ->
                if (keys.isEmpty()) {
                    Napier.v { "There are no keys for the current flow. Returning empty state flow list." }
                    return@flatMapConcat MutableStateFlow(emptyList())
                }
                val flows = keys
                    .filter { it.startsWith("$namespace.") }
                    .map { settings.getStringOrNullFlow(it) }
                combine(flows) { values ->
                    values
                        .filterNotNull()
                        .filter { it.isNotEmpty() }
                        .mapNotNull { decode(it) }
                }
            }
    }

    private fun put(item: T) {
        val key = "${namespace}.${item.id}"
        val data = json.encodeToString(serializer, item)
        settings.putString(key, data)
        _keysFlow.value += key
    }

    override suspend fun insert(item: T): Long {
        put(item)
        return 1L
    }

    override suspend fun insert(items: List<T>) {
        for (item in items) {
            insert(item)
        }
    }

    override suspend fun update(item: T): Long {
        put(item)
        return 1L
    }

    override suspend fun update(items: List<T>) {
        for (item in items) {
            update(item)
        }
    }

    override suspend fun delete(id: IdType) {
        val key = "${namespace}.$id"
        settings.remove(key)
        _keysFlow.value -= key
    }

    override suspend fun deleteByIdList(ids: List<IdType>) {
        for (id in ids) {
            delete(id)
        }
    }
}
