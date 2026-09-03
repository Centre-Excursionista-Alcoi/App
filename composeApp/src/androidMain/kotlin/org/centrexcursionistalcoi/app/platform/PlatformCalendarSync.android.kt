package org.centrexcursionistalcoi.app.platform

import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import com.diamondedge.logging.logging
import org.koin.core.annotation.Singleton
import kotlin.time.Instant

@Singleton
actual class PlatformCalendarSync(private val context: Context) : PlatformProvider {
    private val log = logging()

    actual override val isSupported: Boolean = true

    actual fun addCalendarEvent(
        title: String,
        location: String,
        begin: Instant,
        end: Instant,
        description: String?
    ): Boolean {
        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(CalendarContract.Events.TITLE, title)
            putExtra(CalendarContract.Events.EVENT_LOCATION, location)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, begin.toEpochMilliseconds())
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, end.toEpochMilliseconds())
            putExtra(CalendarContract.Events.DESCRIPTION, description)
            putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, false)
        }

        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
            return true
        } else {
            log.e { "No calendar app is available." }
            return false
        }
    }
}
