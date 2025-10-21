// Copied from: https://github.com/ktorio/ktor/blob/main/ktor-client/ktor-client-core/common/src/io/ktor/client/plugins/cookies/AcceptAllCookiesStorage.kt

package org.centrexcursionistalcoi.app.storage

import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.client.plugins.cookies.fillDefaults
import io.ktor.client.plugins.cookies.matches
import io.ktor.http.Cookie
import io.ktor.http.Url
import io.ktor.util.date.getTimeMillis
import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.math.min
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable

/**
 * [CookiesStorage] that stores all the cookies in an in-memory map.
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.plugins.cookies.AcceptAllCookiesStorage)
 */
@OptIn(ExperimentalAtomicApi::class)
abstract class CookiesStorageImpl(
    private val clock: () -> Long = { getTimeMillis() }
) : CookiesStorage {

    @Serializable
    data class CookieWithTimestamp(val cookie: Cookie, val createdAt: Long)

    private val oldestCookie: AtomicLong = AtomicLong(0L)
    private val mutex = Mutex()

    /**
     * Returns all stored cookies with their creation timestamps.
     */
    abstract suspend fun allCookies(): List<CookieWithTimestamp>

    /**
     * Removes all cookies that match the given [predicate].
     */
    abstract suspend fun removeAll(predicate: (CookieWithTimestamp) -> Boolean)

    /**
     * Stores the given [cookieWithTimestamp].
     */
    abstract suspend fun put(cookieWithTimestamp: CookieWithTimestamp)

    /**
     * Removes all stored cookies.
     */
    abstract suspend fun clearStorage()

    suspend fun clear() = mutex.withLock {
        clearStorage()
        oldestCookie.store(0L)
    }

    override suspend fun get(requestUrl: Url): List<Cookie> = mutex.withLock {
        val now = clock()
        if (now >= oldestCookie.load()) cleanup(now)

        val cookies = allCookies().filter { it.cookie.matches(requestUrl) }.map { it.cookie }
        return@withLock cookies
    }

    override suspend fun addCookie(requestUrl: Url, cookie: Cookie) {
        with(cookie) {
            if (name.isBlank()) return
        }

        mutex.withLock {
            removeAll { (existingCookie, _) ->
                existingCookie.name == cookie.name && existingCookie.matches(requestUrl)
            }
            val createdAt = clock()
            put(CookieWithTimestamp(cookie.fillDefaults(requestUrl), createdAt))

            cookie.maxAgeOrExpires(createdAt)?.let {
                if (oldestCookie.load() > it) {
                    oldestCookie.store(it)
                }
            }
        }
    }

    override fun close() {
    }

    private suspend fun cleanup(timestamp: Long) {
        removeAll { (cookie, createdAt) ->
            val expires = cookie.maxAgeOrExpires(createdAt) ?: return@removeAll false
            expires < timestamp
        }

        val newOldest = allCookies().fold(Long.MAX_VALUE) { acc, (cookie, createdAt) ->
            cookie.maxAgeOrExpires(createdAt)?.let { min(acc, it) } ?: acc
        }

        oldestCookie.store(newOldest)
    }

    private fun Cookie.maxAgeOrExpires(createdAt: Long): Long? =
        maxAge?.let { createdAt + it * 1000L } ?: expires?.timestamp
}
