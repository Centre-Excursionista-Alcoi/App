package org.centrexcursionistalcoi.app

import com.diamondedge.logging.LogLevelController
import com.diamondedge.logging.Logger
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class JvmLogger(logLevel: LogLevelController) : Logger, LogLevelController by logLevel {
    companion object {
        const val ANSI_RESET = "\u001B[0m"
        const val ANSI_RED = "\u001B[31m"
        const val ANSI_YELLOW = "\u001B[33m"
        const val ANSI_CYAN = "\u001B[36m"
        const val ANSI_BLUE = "\u001B[34m"
        const val ANSI_WHITE = "\u001B[37m"
    }

    override fun verbose(tag: String, msg: String) {
        println(message("V", ANSI_WHITE, tag, msg, null))
    }

    override fun debug(tag: String, msg: String) {
        println(message("D", ANSI_BLUE, tag, msg, null))
    }

    override fun info(tag: String, msg: String) {
        println(message("I", ANSI_CYAN, tag, msg, null))
    }

    override fun warn(tag: String, msg: String, t: Throwable?) {
        println(message("W", ANSI_YELLOW, tag, msg, t))
    }

    override fun error(tag: String, msg: String, t: Throwable?) {
        println(message("E", ANSI_RED, tag, msg, t))
    }

    @OptIn(ExperimentalTime::class)
    private fun message(level: String, color: String, tag: String, msg: String, t: Throwable?): String {
        val now = Clock.System.now()
        val str = if (tag.isEmpty()) "$level:" else "$level/$tag:"
        return "$color$now $str $msg ${t?.stackTraceToString() ?: ""}$ANSI_RESET"
    }
}
