import io.github.aakira.napier.Antilog
import io.github.aakira.napier.LogLevel

class DesktopAntilog : Antilog() {
    @Suppress("unused")
    companion object {
        const val ANSI_RESET = "\u001B[0m"
        const val ANSI_BLACK = "\u001B[30m"
        const val ANSI_RED = "\u001B[31m"
        const val ANSI_GREEN = "\u001B[32m"
        const val ANSI_YELLOW = "\u001B[33m"
        const val ANSI_BLUE = "\u001B[34m"
        const val ANSI_PURPLE = "\u001B[35m"
        const val ANSI_CYAN = "\u001B[36m"
        const val ANSI_WHITE = "\u001B[37m"

        const val ANSI_BLACK_BACKGROUND = "\u001B[40m"
        const val ANSI_RED_BACKGROUND = "\u001B[41m"
        const val ANSI_GREEN_BACKGROUND = "\u001B[42m"
        const val ANSI_YELLOW_BACKGROUND = "\u001B[43m"
        const val ANSI_BLUE_BACKGROUND = "\u001B[44m"
        const val ANSI_PURPLE_BACKGROUND = "\u001B[45m"
        const val ANSI_CYAN_BACKGROUND = "\u001B[46m"
        const val ANSI_WHITE_BACKGROUND = "\u001B[47m"
    }

    override fun performLog(
        priority: LogLevel,
        tag: String?,
        throwable: Throwable?,
        message: String?
    ) {
        val foreground = when(priority) {
            LogLevel.VERBOSE, LogLevel.DEBUG -> ANSI_CYAN
            LogLevel.ASSERT, LogLevel.WARNING -> ANSI_YELLOW
            LogLevel.INFO -> ANSI_BLUE
            LogLevel.ERROR -> ANSI_RED
        }
        val (tagBackground, tagForeground) = when(priority) {
            LogLevel.VERBOSE, LogLevel.DEBUG -> ANSI_CYAN_BACKGROUND to ANSI_BLACK
            LogLevel.ASSERT, LogLevel.WARNING -> ANSI_YELLOW_BACKGROUND to ANSI_BLACK
            LogLevel.INFO -> ANSI_BLUE_BACKGROUND to ANSI_BLACK
            LogLevel.ERROR -> ANSI_RED_BACKGROUND to ANSI_WHITE
        }
        val line = StringBuilder().apply {
            append(tagBackground)
            append(tagForeground)
            append(priority.name)
            append(ANSI_RESET)
            append(foreground)
            if (tag != null) {
                append("/ ")
                append(tag)
            }
            append(" - ")
            append(message)
        }
        println(line)
        throwable?.let {
            println()
            it.printStackTrace()
        }
    }
}
