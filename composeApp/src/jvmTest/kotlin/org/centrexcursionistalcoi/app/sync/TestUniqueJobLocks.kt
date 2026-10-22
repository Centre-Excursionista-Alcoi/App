package org.centrexcursionistalcoi.app.sync

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class TestUniqueJobLocks {
    @Test
    fun `jobs with the same unique name run one after the other`() = runTest {
        val events = mutableListOf<String>()
        val release = CompletableDeferred<Unit>()

        val first = async {
            UniqueJobLocks.serialized("same") {
                events += "first start"
                release.await()
                events += "first end"
            }
        }
        advanceUntilIdle()
        val second = async {
            UniqueJobLocks.serialized("same") { events += "second" }
        }
        advanceUntilIdle()
        assertEquals(listOf("first start"), events, "The second job must wait for the first")

        release.complete(Unit)
        first.await()
        second.await()
        assertEquals(listOf("first start", "first end", "second"), events)
    }

    @Test
    fun `jobs with different or no unique name don't wait for each other`() = runTest {
        val release = CompletableDeferred<Unit>()
        val blocker = async { UniqueJobLocks.serialized("a") { release.await() } }
        advanceUntilIdle()

        assertEquals("b", UniqueJobLocks.serialized("b") { "b" })
        assertEquals("none", UniqueJobLocks.serialized(null) { "none" })

        release.complete(Unit)
        blocker.await()
    }

    @Test
    fun `a failing job releases the lock`() = runTest {
        runCatching { UniqueJobLocks.serialized("fail") { error("boom") } }
        assertEquals("ok", UniqueJobLocks.serialized("fail") { "ok" })
    }
}
