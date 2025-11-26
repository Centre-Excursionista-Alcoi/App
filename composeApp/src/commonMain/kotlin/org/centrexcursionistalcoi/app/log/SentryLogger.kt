package org.centrexcursionistalcoi.app.log

import com.diamondedge.logging.Logger
import io.sentry.kotlin.multiplatform.Sentry
import io.sentry.kotlin.multiplatform.SentryLevel
import io.sentry.kotlin.multiplatform.protocol.Breadcrumb

class SentryLogger : Logger {
    private fun addBreadcrumb(level: SentryLevel, tag: String, msg: String) {
        Sentry.addBreadcrumb(
            Breadcrumb(
                level = level,
                message = "[$tag] $msg",
            )
        )
    }

    override fun verbose(tag: String, msg: String) {
        addBreadcrumb(SentryLevel.INFO, tag, msg)
    }

    override fun debug(tag: String, msg: String) {
        addBreadcrumb(SentryLevel.DEBUG, tag, msg)
    }

    override fun info(tag: String, msg: String) {
        addBreadcrumb(SentryLevel.INFO, tag, msg)
    }

    override fun warn(tag: String, msg: String, t: Throwable?) {
        addBreadcrumb(SentryLevel.WARNING, tag, msg)
    }

    override fun error(tag: String, msg: String, t: Throwable?) {
        addBreadcrumb(SentryLevel.ERROR, tag, msg)
        if (t != null) {
            Sentry.captureException(t)
        }
    }

    override fun isLoggingVerbose(): Boolean = true

    override fun isLoggingDebug(): Boolean = true

    override fun isLoggingInfo(): Boolean = true

    override fun isLoggingWarning(): Boolean = true

    override fun isLoggingError(): Boolean = true
}
