package org.centrexcursionistalcoi.app.platform

import org.koin.core.annotation.Singleton
import kotlin.time.Instant

@Singleton
expect class PlatformCalendarSync : PlatformProvider {
    override val isSupported: Boolean

    fun addCalendarEvent(title: String, location: String, begin: Instant, end: Instant, description: String?): Boolean
}
