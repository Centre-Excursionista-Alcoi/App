package org.centrexcursionistalcoi.app.sync

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * One [Mutex] per `uniqueName`, so jobs scheduled with the same one run one after the other.
 *
 * WorkManager enforces that on Android, but [BackgroundJobCoordinator] here launches a coroutine per call: without
 * this, two syncs could run at once and race on the same rows (`UNIQUE constraint failed: Users.sub`).
 */
@PublishedApi
internal object UniqueJobLocks {
    private val registryLock = Mutex()
    private val locks = mutableMapOf<String, Mutex>()

    @PublishedApi
    internal suspend fun lockFor(uniqueName: String): Mutex = registryLock.withLock {
        locks.getOrPut(uniqueName) { Mutex() }
    }

    /** Runs [block] once no other job with the same [uniqueName] is running. Jobs without one aren't serialized. */
    @PublishedApi
    internal suspend inline fun <T> serialized(uniqueName: String?, block: () -> T): T {
        if (uniqueName == null) return block()
        return lockFor(uniqueName).withLock { block() }
    }
}
