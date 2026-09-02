package org.centrexcursionistalcoi.app.datetime

import kotlinx.datetime.UtcOffset
import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.format.DateTimeFormat
import kotlinx.datetime.format.char

val DateTimeComponents.Formats.RFC_9557: DateTimeFormat<DateTimeComponents>
    get() = DateTimeComponents.Format {
        year()
        char('-')
        monthNumber()
        char('-')
        day()
        char('T')
        hour()
        char(':')
        minute()
        char(':')
        second()
        offset(UtcOffset.Formats.ISO)
        char('[')
        timeZoneId()
        char(']')
    }
