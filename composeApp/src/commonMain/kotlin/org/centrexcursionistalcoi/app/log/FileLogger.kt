package org.centrexcursionistalcoi.app.log

import com.diamondedge.logging.Logger
import io.ktor.utils.io.core.*
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import org.centrexcursionistalcoi.app.BuildKonfig
import org.centrexcursionistalcoi.app.utils.toLocalDateTime
import kotlin.time.Clock

class FileLogger(private val filePath: Path) : Logger {
    private enum class Level(val char: Char) {
        VERBOSE('V'), DEBUG('D'), INFO('I'), WARN('W'), ERROR('E')
    }

    private fun log(level: Level, tag: String, msg: String, t: Throwable?) {
        val timestamp = Clock.System.now().toLocalDateTime().toString()
        SystemFileSystem.sink(filePath, true).buffered().use { sink ->
            sink.writeText(
                "${level.char}/$tag $timestamp: $msg${t?.let { "\n${it.stackTraceToString()}" } ?: ""}\n"
            )
        }
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
        // Append empty lines on boot
        SystemFileSystem.sink(filePath, true).buffered().use { sink ->
            sink.writeText("\n\n\n")
            sink.writeText("--- App started at ${Clock.System.now().toLocalDateTime()} ---\n")
            sink.writeText("    Version: ${BuildKonfig.VERSION_NAME}/${BuildKonfig.VERSION_CODE}\n")
            sink.writeText("    Is Debug: ${BuildKonfig.DEBUG}\n")
            sink.writeText("----------------------------------------------------\n")
        }
    }
}
