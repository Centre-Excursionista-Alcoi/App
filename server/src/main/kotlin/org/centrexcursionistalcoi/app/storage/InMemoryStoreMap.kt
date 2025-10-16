package org.centrexcursionistalcoi.app.storage

import java.util.concurrent.ConcurrentHashMap

class InMemoryStoreMap : StoreMap {
    private val map = ConcurrentHashMap<String, String>()

    override suspend fun put(key: String, value: String) {
        map[key] = value
    }

    override suspend fun remove(key: String): String? {
        return map.remove(key)
    }

    override fun close() {
        // No resources to close for in-memory store
    }

    override suspend fun clear() {
        map.clear()
    }
}
