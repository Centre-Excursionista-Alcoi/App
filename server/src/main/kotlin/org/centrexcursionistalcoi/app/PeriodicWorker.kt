package org.centrexcursionistalcoi.app

import java.io.Closeable
import kotlin.coroutines.CoroutineContext
import kotlin.time.Clock
import kotlin.time.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import org.slf4j.LoggerFactory

abstract class PeriodicWorker(
    private val period: Duration,
    context: CoroutineContext = Dispatchers.IO,
): Closeable {
    private val logger = LoggerFactory.getLogger(this::class.java)

    private val scope = CoroutineScope(context)
    private val mutex = Mutex() // Prevents overlapping syncs
    private var job: Job? = null

    /**
     * Starts the periodic worker.
     *
     * This function launches a coroutine that runs the [run] method at fixed intervals defined by [period].
     * If the previous run is still active, the next run is skipped to prevent overlapping executions.
     *
     * The worker continues to run until [stop] is called or the coroutine is cancelled.
     *
     * [run] will be called once when this function is invoked.
     */
    fun start() {
        job = scope.launch {
            while (isActive) {
                // Try to acquire lock; if busy, skip this interval
                if (mutex.tryLock()) {
                    try {
                        logger.debug("Starting sync: {}", Clock.System.now())
                        run()
                    } finally {
                        mutex.unlock()
                    }
                } else {
                    logger.debug("Skipping sync: Previous run still active.")
                }

                // Wait for next interval
                delay(period)
            }
        }
    }

    /**
     * Stops the periodic worker.
     *
     * This function cancels the running coroutine, stopping any further executions of [run].
     */
    fun stop() {
        job?.cancel()
        job = null
    }

    /**
     * Closes the periodic worker by stopping it.
     *
     * Alias for [stop].
     */
    override fun close() {
        stop()
    }

    /**
     * The task to be performed periodically.
     */
    protected abstract suspend fun run()
}
