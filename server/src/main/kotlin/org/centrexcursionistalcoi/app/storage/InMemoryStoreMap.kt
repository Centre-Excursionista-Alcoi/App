package org.centrexcursionistalcoi.app.storage

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class InMemoryStoreMap : StoreMap {
    private val map = ConcurrentHashMap<String, String>()
    override suspend fun keys(): Set<String> = map.keys.toSet()

    override suspend fun put(key: String, value: String) {
        map[key] = value
    }

    override suspend fun put(key: String, value: String, expirationSeconds: Long) {
        map[key] = value
        // Best-effort expiration to mirror RedisStoreMap's TTL -- no expiration if the process dies first, which
        // is fine here: this backend only exists as a fallback for local/dev use without Redis configured.
        CoroutineScope(Dispatchers.Default).launch {
            delay(expirationSeconds * 1000)
            map.remove(key, value)
        }
    }

    override suspend fun get(key: String): String? {
        return map[key]
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
