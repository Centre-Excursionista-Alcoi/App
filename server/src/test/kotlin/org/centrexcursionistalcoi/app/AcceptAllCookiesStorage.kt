// Copied from: https://github.com/ktorio/ktor/blob/main/ktor-client/ktor-client-core/common/src/io/ktor/client/plugins/cookies/AcceptAllCookiesStorage.kt

package org.centrexcursionistalcoi.app

import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.client.plugins.cookies.fillDefaults
import io.ktor.client.plugins.cookies.matches
import io.ktor.http.Cookie
import io.ktor.http.Url
import io.ktor.util.date.getTimeMillis
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.math.min

/**
 * [CookiesStorage] that stores all the cookies in an in-memory map.
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.plugins.cookies.AcceptAllCookiesStorage)
 */
@OptIn(ExperimentalAtomicApi::class)
class AcceptAllCookiesStorage(
    private val clock: () -> Long = { getTimeMillis() }
) : CookiesStorage {

    private data class CookieWithTimestamp(val cookie: Cookie, val createdAt: Long)

    private val container: MutableList<CookieWithTimestamp> = mutableListOf()
    private val oldestCookie: AtomicLong = AtomicLong(0L)
    private val mutex = Mutex()

    suspend fun clear() = mutex.withLock {
        container.clear()
        oldestCookie.store(0L)
    }

    override suspend fun get(requestUrl: Url): List<Cookie> = mutex.withLock {
        val now = clock()
        if (now >= oldestCookie.load()) cleanup(now)

        val cookies = container.filter { it.cookie.matches(requestUrl) }.map { it.cookie }
        return@withLock cookies
    }

    override suspend fun addCookie(requestUrl: Url, cookie: Cookie) {
        with(cookie) {
            if (name.isBlank()) return
        }

        mutex.withLock {
            container.removeAll { (existingCookie, _) ->
                existingCookie.name == cookie.name && existingCookie.matches(requestUrl)
            }
            val createdAt = clock()
            container.add(CookieWithTimestamp(cookie.fillDefaults(requestUrl), createdAt))

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
        container.removeAll { (cookie, createdAt) ->
            val expires = cookie.maxAgeOrExpires(createdAt) ?: return@removeAll false
            expires < timestamp
        }

        val newOldest = container.fold(Long.MAX_VALUE) { acc, (cookie, createdAt) ->
            cookie.maxAgeOrExpires(createdAt)?.let { min(acc, it) } ?: acc
        }

        oldestCookie.store(newOldest)
    }

    private fun Cookie.maxAgeOrExpires(createdAt: Long): Long? =
        maxAge?.let { createdAt + it * 1000L } ?: expires?.timestamp
}
