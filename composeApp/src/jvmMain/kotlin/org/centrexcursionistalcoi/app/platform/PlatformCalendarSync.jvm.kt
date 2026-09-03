package org.centrexcursionistalcoi.app.platform

import org.koin.core.annotation.Singleton
import kotlin.time.Instant

@Singleton
actual class PlatformCalendarSync : PlatformProvider {
    actual override val isSupported: Boolean = false

    actual fun addCalendarEvent(
        title: String,
        location: String,
        begin: Instant,
        end: Instant,
        description: String?
    ): Boolean {
        throw UnsupportedOperationException("Calendar sync is not supported on JVM.")
    }
}
