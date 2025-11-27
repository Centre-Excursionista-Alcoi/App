package org.centrexcursionistalcoi.app.data

import androidx.compose.runtime.Composable
import cea_app.composeapp.generated.resources.*
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
        stringResource(Res.string.event_date_with_end, startStr)
    } else {
        stringResource(Res.string.event_date_no_end, startStr)
    }
}
