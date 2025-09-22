package org.centrexcursionistalcoi.app.storage

import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.client.plugins.cookies.fillDefaults
import io.ktor.client.plugins.cookies.matches
import io.ktor.http.Cookie
import io.ktor.http.Url
import io.ktor.util.date.getTimeMillis
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.json
import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.math.min

@OptIn(ExperimentalAtomicApi::class)
class SettingsCookiesStorage(
    private val clock: () -> Long = { getTimeMillis() }
) : CookiesStorage {
    @Serializable
    private data class CookieWithTimestamp(val cookie: Cookie, val createdAt: Long)

    private val oldestCookie: AtomicLong = AtomicLong(0L)
    private val mutex = Mutex()

    private fun getCookies(): Map<String, CookieWithTimestamp> {
        return settings.keys
            .filter { it.startsWith("cookie.") }
            .mapNotNull { settings.getStringOrNull(it) }
            .associateWith { json.decodeFromString(CookieWithTimestamp.serializer(), it) }
    }

    suspend fun clear() = mutex.withLock {
        settings.keys
            .filter { it.startsWith("cookie.") }
            .forEach { settings.remove(it) }
        oldestCookie.store(0L)
    }

    override suspend fun get(requestUrl: Url): List<Cookie> = mutex.withLock {
        val now = clock()
        if (now >= oldestCookie.load()) cleanup(now)

        val cookies = getCookies().values.filter { it.cookie.matches(requestUrl) }.map { it.cookie }
        return@withLock cookies
    }

    override suspend fun addCookie(requestUrl: Url, cookie: Cookie) {
        with(cookie) {
            if (name.isBlank()) return
        }

        mutex.withLock {
            getCookies().filter { (_, value) ->
                value.cookie.name == cookie.name && value.cookie.matches(requestUrl)
            }.forEach { (k) -> settings.remove(k) }
            val createdAt = clock()
            settings.putString(
                "cookie.${cookie.name}-${createdAt}",
                json.encodeToString(
                    CookieWithTimestamp.serializer(),
                    CookieWithTimestamp(cookie.fillDefaults(requestUrl), createdAt)
                )
            )

            cookie.maxAgeOrExpires(createdAt)?.let {
                if (oldestCookie.load() > it) {
                    oldestCookie.store(it)
                }
            }
        }
    }

    override fun close() {
    }

    private fun cleanup(timestamp: Long) {
        getCookies().filter { (_, value) ->
            val expires = value.cookie.maxAgeOrExpires(value.createdAt) ?: return@filter false
            expires < timestamp
        }.forEach { (k) -> settings.remove(k) }

        val newOldest = getCookies().values.fold(Long.MAX_VALUE) { acc, (cookie, createdAt) ->
            cookie.maxAgeOrExpires(createdAt)?.let { min(acc, it) } ?: acc
        }

        oldestCookie.store(newOldest)
    }

    private fun Cookie.maxAgeOrExpires(createdAt: Long): Long? =
        maxAge?.let { createdAt + it * 1000L } ?: expires?.timestamp
}
