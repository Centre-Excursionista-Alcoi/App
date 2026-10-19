package org.centrexcursionistalcoi.app.log

import com.diamondedge.logging.Logger
import io.ktor.utils.io.core.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import org.centrexcursionistalcoi.app.BuildKonfig
import org.centrexcursionistalcoi.app.utils.toLocalDateTime
import kotlin.time.Clock

/**
 * Appends the logs to [filePath], from a background coroutine so that logging never blocks the caller.
 *
 * The file is kept below [maxFileBytes]: when a write would exceed it, the file is moved to `<filePath>.1` (replacing
 * the previous one), so at most two files exist. Each message is cut at [maxMessageChars], since a single huge entry
 * (e.g. a request with a photo) would otherwise fill the file.
 */
class FileLogger(
    private val filePath: Path,
    private val maxFileBytes: Long = MAX_FILE_BYTES,
    private val maxMessageChars: Int = MAX_MESSAGE_CHARS,
) : Logger {
    private enum class Level(val char: Char) {
        VERBOSE('V'), DEBUG('D'), INFO('I'), WARN('W'), ERROR('E')
    }

    private sealed interface Entry {
        class Text(val text: String) : Entry
        class Flush(val done: CompletableDeferred<Unit>) : Entry
    }

    private val queue = Channel<Entry>(Channel.UNLIMITED)
    // Dispatchers.IO isn't available in common code. A single thread writes, so lines are never interleaved
    @OptIn(ExperimentalCoroutinesApi::class)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default.limitedParallelism(1))

    private val rotatedPath = Path("$filePath.1")

    private var currentSize: Long = sizeOf(filePath)

    /**
     * Waits until everything logged until now is written. Meant for tests and for before exiting.
     */
    suspend fun flush() {
        val done = CompletableDeferred<Unit>()
        queue.send(Entry.Flush(done))
        done.await()
    }

    private fun sizeOf(path: Path): Long = try {
        SystemFileSystem.metadataOrNull(path)?.size ?: 0L
    } catch (_: Exception) {
        0L
    }

    private fun write(text: String) {
        try {
            val bytes = text.encodeToByteArray()
            if (currentSize > 0 && currentSize + bytes.size > maxFileBytes) {
                if (SystemFileSystem.exists(rotatedPath)) SystemFileSystem.delete(rotatedPath)
                SystemFileSystem.atomicMove(filePath, rotatedPath)
                currentSize = 0
            }
            SystemFileSystem.sink(filePath, append = true).buffered().use { it.write(bytes) }
            currentSize += bytes.size
        } catch (_: Exception) {
            // Logging must never crash the app
        }
    }

    private fun log(level: Level, tag: String, msg: String, t: Throwable?) {
        val timestamp = Clock.System.now().toLocalDateTime().toString()
        val body = "$msg${t?.let { "\n${it.stackTraceToString()}" } ?: ""}"
        val truncated = if (body.length > maxMessageChars) {
            "${body.take(maxMessageChars)}… [truncated, ${body.length} characters]"
        } else {
            body
        }
        queue.trySend(Entry.Text("${level.char}/$tag $timestamp: $truncated\n"))
    }

    override fun verbose(tag: String, msg: String) {
        log(Level.VERBOSE, tag, msg, null)
    }

    override fun debug(tag: String, msg: String) {
        log(Level.DEBUG, tag, msg, null)
    }

    override fun info(tag: String, msg: String) {
        log(Level.INFO, tag, msg, null)
    }

    override fun warn(tag: String, msg: String, t: Throwable?) {
        log(Level.WARN, tag, msg, t)
    }

    override fun error(tag: String, msg: String, t: Throwable?) {
        log(Level.ERROR, tag, msg, t)
    }

    override fun isLoggingVerbose(): Boolean = true

    override fun isLoggingDebug(): Boolean = true

    override fun isLoggingInfo(): Boolean = true

    override fun isLoggingWarning(): Boolean = true

    override fun isLoggingError(): Boolean = true

    init {
        scope.launch {
            for (entry in queue) {
                when (entry) {
                    is Entry.Text -> write(entry.text)
                    is Entry.Flush -> entry.done.complete(Unit)
                }
            }
        }

        queue.trySend(
            Entry.Text(
                "\n--- App started at ${Clock.System.now().toLocalDateTime()} ---\n" +
                    "    Version: ${BuildKonfig.VERSION_NAME}/${BuildKonfig.VERSION_CODE}\n" +
                    "    Is Debug: ${BuildKonfig.DEBUG}\n" +
                    "----------------------------------------------------\n"
            )
        )
    }

    companion object {
        /** The size at which the file is rotated. */
        const val MAX_FILE_BYTES = 5L * 1024 * 1024

        /** The length at which a message is cut. */
        const val MAX_MESSAGE_CHARS = 8 * 1024
    }
}
