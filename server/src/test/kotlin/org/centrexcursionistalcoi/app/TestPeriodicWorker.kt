package org.centrexcursionistalcoi.app

import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds

class TestPeriodicWorker {
    private class CountingWorker(period: kotlin.time.Duration) : PeriodicWorker(period) {
        val runs = AtomicInteger()

        override suspend fun run() {
            runs.incrementAndGet()
        }
    }

    private fun waitUntil(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 2_000
        while (!condition() && System.currentTimeMillis() < deadline) Thread.sleep(10)
    }

    @Test
    fun `waiting for the first sync runs it once, not again right away`() {
        val worker = CountingWorker(1.hours)

        worker.start(waitUntilFirstSync = true)
        // Time for a wrong second run to happen
        Thread.sleep(300)

        assertEquals(1, worker.runs.get())
        worker.close()
    }

    @Test
    fun `without waiting it runs once, right away`() {
        val worker = CountingWorker(1.hours)

        worker.start()
        waitUntil { worker.runs.get() >= 1 }
        Thread.sleep(300)

        assertEquals(1, worker.runs.get())
        worker.close()
    }

    @Test
    fun `after waiting for the first sync it keeps running every period`() {
        val worker = CountingWorker(100.milliseconds)

        worker.start(waitUntilFirstSync = true)
        waitUntil { worker.runs.get() >= 3 }

        assertEquals(true, worker.runs.get() >= 3)
        worker.close()
    }

    @Test
    fun `starting twice does not run the first sync twice`() {
        val worker = CountingWorker(1.hours)

        worker.start(waitUntilFirstSync = true)
        worker.start(waitUntilFirstSync = true)
        Thread.sleep(300)

        assertEquals(1, worker.runs.get())
        worker.close()
    }
}
