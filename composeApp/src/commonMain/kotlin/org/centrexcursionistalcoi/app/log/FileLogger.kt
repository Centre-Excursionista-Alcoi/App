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

    /**
     * Reads the log file content and returns it as a list of lines.
     */
    private fun readLogContent(): List<String> {
        return try {
            SystemFileSystem.source(filePath).buffered().use { source ->
                source.readText().lines()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Writes the lines given into the log file.
     */
    private fun writeLogContent(lines: List<String>) {
        SystemFileSystem.sink(filePath, false).buffered().use { sink ->
            sink.writeText(lines.joinToString("\n") + "\n")
        }
    }

    private fun log(level: Level, tag: String, msg: String, t: Throwable?) {
        val timestamp = Clock.System.now().toLocalDateTime().toString()
        val newLine = "${level.char}/$tag $timestamp: $msg${t?.let { "\n${it.stackTraceToString()}" } ?: ""}"

        val existingLines = readLogContent()
        val allLines = (existingLines + newLine).filter { it.isNotBlank() }.takeLast(MAX_LINES)
        writeLogContent(allLines)
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
        val startupLines = listOf(
            "",
            "--- App started at ${Clock.System.now().toLocalDateTime()} ---",
            "    Version: ${BuildKonfig.VERSION_NAME}/${BuildKonfig.VERSION_CODE}",
            "    Is Debug: ${BuildKonfig.DEBUG}",
            "----------------------------------------------------"
        )

        val existingLines = readLogContent()
        val allLines = (existingLines + startupLines).filter { it.isNotBlank() }.takeLast(MAX_LINES)
        writeLogContent(allLines)
    }

    companion object {
        private const val MAX_LINES = 10000
    }
}
