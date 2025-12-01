package org.centrexcursionistalcoi.app.data

import androidx.compose.runtime.Composable
import cea_app.composeapp.generated.resources.Res
import cea_app.composeapp.generated.resources.date_from
import cea_app.composeapp.generated.resources.date_until
import org.centrexcursionistalcoi.app.platform.PlatformCalendarSync
import org.centrexcursionistalcoi.app.utils.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun ReferencedEvent.localizedDateRange(): String {
    val end = end // Make it immutable
    val sameDay = end?.let { start.isSameDayAs(it) } ?: false
    return if (sameDay) {
        val date = start.toLocalDateTime().date
        val fromTime = start.toLocalDateTime().time
        val toTime = end.toLocalDateTime().time
        return localizedDateWithTimeRange(date, fromTime, toTime)
    } else {
        if (end == null) {
            localizedDateTime(start.toLocalDateTime())
        } else {
            val startStr = localizedInstantAsDateTime(start)
            val endStr = localizedInstantAsDateTime(end)
            return stringResource(Res.string.date_from, startStr) + '\n' + stringResource(Res.string.date_until, endStr)
        }
    }
}

fun PlatformCalendarSync.addCalendarEvent(event: ReferencedEvent): Boolean {
    val title = event.title
    val description = event.description
    val place = event.place
    val startMillis = event.start
    val endMillis = event.end ?: event.start // If no end is provided, use start time as end time

    return addCalendarEvent(
        title = title,
        description = description,
        location = place,
        begin = startMillis,
        end = endMillis
    )
}
