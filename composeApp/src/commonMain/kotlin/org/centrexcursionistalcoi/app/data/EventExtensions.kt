package org.centrexcursionistalcoi.app.data

import androidx.compose.runtime.Composable
import cea_app.composeapp.generated.resources.*
import org.centrexcursionistalcoi.app.platform.PlatformCalendarSync
import org.centrexcursionistalcoi.app.utils.localizedInstantAsDateTime
import org.jetbrains.compose.resources.stringResource

@Composable
fun ReferencedEvent.localizedStartDate(): String {
    return localizedInstantAsDateTime(start)
}

@Composable
fun ReferencedEvent.localizedEndDate(): String? {
    val end = end ?: return null
    return localizedInstantAsDateTime(end)
}

@Composable
fun ReferencedEvent.localizedDateRange(): String {
    val startStr = localizedInstantAsDateTime(start)
    val endStr = end?.let { localizedInstantAsDateTime(it) }
    return if (endStr != null) {
        stringResource(Res.string.event_date_with_end, startStr, endStr)
    } else {
        stringResource(Res.string.event_date_no_end, startStr)
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
