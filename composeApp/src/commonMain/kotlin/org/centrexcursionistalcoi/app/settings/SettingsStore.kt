package org.centrexcursionistalcoi.app.settings

import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationException
import kotlinx.serialization.SerializationStrategy
import org.centrexcursionistalcoi.app.json
import org.koin.core.annotation.Singleton
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

@Singleton
class SettingsStore {
    private val store = createDataStore()

    suspend fun keys(): Set<Preferences.Key<*>> {
        return store.data.map { preferences -> preferences.asMap().keys }.first()
    }

    suspend fun <T> get(key: Preferences.Key<T>): T? {
        return store.data.map { preferences -> preferences[key] }.first()
    }

    /**
     * Gets a value from the settings store and deserializes it using the provided [serializer].
     * @param key The key to retrieve the value from.
     * @param serializer The serializer to use for deserialization.
     * @return The deserialized value, or null if the key does not exist.
     * @throws IllegalArgumentException if the value cannot be deserialized.
     * @throws SerializationException if the value cannot be deserialized.
     */
    suspend fun <T: Any> get(key: Preferences.Key<String>, serializer: DeserializationStrategy<T>): T? {
        val raw = get(key) ?: return null
        return json.decodeFromString(serializer, raw)
    }

    suspend fun <T> get(key: Preferences.Key<T>, default: T): T {
        return store.data.map { preferences -> preferences[key] ?: default }.first()
    }

    fun <T> getBlocking(key: Preferences.Key<T>, default: T): T {
        return runBlocking { get(key, default) }
    }

    fun <T> getFlow(key: Preferences.Key<T>) = store.data.map { preferences -> preferences[key] }

    fun <T> getFlow(key: Preferences.Key<T>, default: T) = store.data.map { preferences -> preferences[key] ?: default }

    fun <T> getFlow(key: Preferences.Key<String>, serializer: DeserializationStrategy<T>): Flow<T?> = getFlow(key).map { string ->
        string?.let { json.decodeFromString(serializer, it) }
    }

    suspend fun <T> set(key: Preferences.Key<T>, value: T) {
        store.updateData { preferences ->
            preferences.toMutablePreferences().also { preferences ->
                preferences[key] = value
            }
        }
    }

    /**
     * Sets a value in the settings store after serializing it using the provided [serializer].
     * @param key The key to store the value under.
     * @param serializer The serializer to use for serialization.
     * @param value The value to serialize and store.
     * @throws SerializationException if the value cannot be serialized.
     */
    suspend fun <T: Any> set(key: Preferences.Key<String>, serializer: SerializationStrategy<T>, value: T) {
        val string = json.encodeToString(serializer, value)
        set(key, string)
    }

    suspend fun remove(key: Preferences.Key<*>) {
        store.updateData { preferences ->
            preferences.toMutablePreferences().also { preferences ->
                preferences.remove(key)
            }
        }
    }

    /**
     * Removes every setting, except the device's own (named [DEVICE_KEY_PREFIX]...), which aren't about the account
     * logged in, and must outlive logging out and in (e.g. when the user was last asked to create a passkey).
     */
    suspend fun clear() {
        store.updateData { preferences ->
            preferences.toMutablePreferences().also { preferences ->
                preferences.asMap().keys
                    .filterNot { it.name.startsWith(DEVICE_KEY_PREFIX) }
                    .forEach { preferences.remove(it) }
            }
        }
    }

    companion object {
        /** The prefix of the names of the settings [clear] keeps. */
        const val DEVICE_KEY_PREFIX = "device."
    }
}

private object SettingsStoreHolder : KoinComponent {
    val store: SettingsStore get() = get()
}

/**
 * Accessor for [globalSettingsStore] from places that can't take constructor injection (top-level
 * objects/functions). Prefer constructor injection wherever the call site is a class Koin can build.
 */
val globalSettingsStore: SettingsStore get() = SettingsStoreHolder.store
