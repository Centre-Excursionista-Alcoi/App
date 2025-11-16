package org.centrexcursionistalcoi.app.utils

import kotlinx.datetime.LocalTime

fun LocalTime.withoutSeconds(): LocalTime {
    return LocalTime(hour, minute)
}
