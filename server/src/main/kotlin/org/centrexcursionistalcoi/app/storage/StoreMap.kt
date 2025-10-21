package org.centrexcursionistalcoi.app.storage

import io.ktor.client.plugins.cookies.CookiesStorage
import java.io.Closeable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

interface StoreMap: Closeable {
    suspend fun keys(): Set<String>
    suspend fun put(key: String, value: String)
    suspend fun get(key: String): String?
    suspend fun remove(key: String): String?

    suspend fun clear()

    fun asCookiesStorage(json: Json = org.centrexcursionistalcoi.app.json): CookiesStorage {
        return object : CookiesStorageImpl() {
            private val CookieWithTimestamp.id: String get() = createdAt.toString()

            override suspend fun allCookies(): List<CookieWithTimestamp> {
                return keys()
                    .mapNotNull { get(it) }
                    .mapNotNull {
                        try {
                            json.decodeFromString(CookieWithTimestamp.serializer(), it)
                        } catch (_: SerializationException) {
                            // Skip invalid entries
                            null
                        }
                    }
            }

            override suspend fun removeAll(predicate: (CookieWithTimestamp) -> Boolean) {
                allCookies()
                    .filter(predicate)
                    .forEach { remove(it.id) }
            }

            override suspend fun put(cookieWithTimestamp: CookieWithTimestamp) {
                val serialized = json.encodeToString(CookieWithTimestamp.serializer(), cookieWithTimestamp)
                put(cookieWithTimestamp.id, serialized)
            }

            override suspend fun clearStorage() {
                removeAll { true }
            }
        }
    }
}
