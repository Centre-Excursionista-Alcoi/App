package org.centrexcursionistalcoi.app.storage

import io.github.crackthecodeabhi.kreds.connection.Endpoint
import io.github.crackthecodeabhi.kreds.connection.newClient

class RedisStoreMap(endpoint: String) : StoreMap {
    companion object {
        fun fromEnvOrNull(): RedisStoreMap? {
            val endpoint = System.getenv("REDIS_ENDPOINT") ?: return null
            return RedisStoreMap(endpoint)
        }
    }

    private val client = newClient(Endpoint.from(endpoint))

    override suspend fun put(key: String, value: String) {
        client.set(key, value)
    }

    override suspend fun remove(key: String): String? {
        return client.get(key).also { client.del(key) }
    }

    override fun close() {
        client.close()
    }

    override suspend fun clear() {
        client.flushAll()
    }
}
