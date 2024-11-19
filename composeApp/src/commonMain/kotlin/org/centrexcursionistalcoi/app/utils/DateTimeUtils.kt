package org.centrexcursionistalcoi.app.utils

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant

/**
 * Converts this [LocalDate] to the number of milliseconds since the Unix epoch of 1970-01-01T00:00:00Z.
 *
 * @param timeZone The time zone to use when converting this date to an instant.
 * @param hour The hour of the day to use when converting this date to an instant.
 * @param minute The minute of the hour to use when converting this date to an instant.
 * @param second The second of the minute to use when converting this date to an instant.
 * @param nanosecond The nanosecond of the second to use when converting this date to an instant.
 *
 * @return The number of milliseconds since the Unix epoch of 1970-01-01T00:00:00Z.
 */
fun LocalDate.toEpochMilliseconds(
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
    hour: Int = 0,
    minute: Int = 0,
    second: Int = 0,
    nanosecond: Int = 0
): Long {
    return atTime(hour, minute, second, nanosecond)
        .toInstant(timeZone)
        .toEpochMilliseconds()
}

/**
 * Converts this [LocalDate] to the number of milliseconds since the Unix epoch at the end of the given day.
 * The end is considered to be 23:59:59.999999999. Daylight saving time changes are taken into account.
 *
 * @param timeZone The time zone to use when converting this date to an instant.
 *
 * @return The number of milliseconds since the Unix epoch of 1970-01-01T00:00:00Z.
 */
fun LocalDate.atEndOfDayInMilliseconds(
    timeZone: TimeZone = TimeZone.currentSystemDefault()
): Long {
    return toEpochMilliseconds(timeZone, 23, 59, 59, 999_999_999)
}
