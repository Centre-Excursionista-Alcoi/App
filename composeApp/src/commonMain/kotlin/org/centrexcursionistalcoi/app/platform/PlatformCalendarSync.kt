package org.centrexcursionistalcoi.app.platform

import kotlin.time.Instant

expect object PlatformCalendarSync : PlatformProvider {
    override val isSupported: Boolean

    fun addCalendarEvent(title: String, location: String, begin: Instant, end: Instant, description: String?): Boolean
}
