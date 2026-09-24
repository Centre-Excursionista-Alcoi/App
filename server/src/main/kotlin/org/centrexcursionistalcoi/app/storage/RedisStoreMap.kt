package org.centrexcursionistalcoi.app.storage

import redis.clients.jedis.RedisClient
import redis.clients.jedis.params.SetParams

class RedisStoreMap(host: String, port: Int) : StoreMap {
    companion object {
        @Volatile
        private var envInstance: RedisStoreMap? = null

        fun fromEnvOrNull(): RedisStoreMap? = envInstance ?: synchronized(this) {
            val endpoint = System.getenv("REDIS_ENDPOINT") ?: return null
            val host = endpoint.substringBefore(":")
            val port = endpoint.substringAfter(":", "6379").toIntOrNull() ?: error("Invalid REDIS_ENDPOINT format. Expected 'host:port', got '$endpoint'")
            return RedisStoreMap(host, port).also { envInstance = it }
        }

        val fromEnv: StoreMap by lazy { fromEnvOrNull() ?: InMemoryStoreMap() }

        val default: StoreMap get() = fromEnv
    }

    private val client = RedisClient.Builder().hostAndPort(host, port).build()

    override suspend fun keys(): Set<String> {
        return client.keys("*").toSet()
    }

    override suspend fun put(key: String, value: String) {
        client.set(key, value)
    }

    override suspend fun put(key: String, value: String, expirationSeconds: Long) {
        SetParams.setParams().ex(expirationSeconds).let { params ->
            client.set(key, value, params)
        }
    }

    override suspend fun get(key: String): String? {
        return client.get(key)
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
