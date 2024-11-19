package org.centrexcursionistalcoi.app.utils

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
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
 * Converts this [LocalDate] to the number of milliseconds since the Unix epoch at the start of the given day.
 *
 * Note that it's not equivalent to `atTime(0, 0).toInstant(timeZone)` because a day does not always start at a fixed
 * time `00:00:00`. For example, if, due to daylight saving time, clocks were shifted from `23:30` of one day directly
 * to `00:30` of the next day, skipping the midnight, then `atStartOfDayIn` would return the [Instant] corresponding to
 * `00:30`, whereas `atTime(0, 0).toInstant(timeZone)` would return the [Instant] corresponding to `01:00`.
 */
fun LocalDate.atStartOfDayInMilliseconds(
    timeZone: TimeZone = TimeZone.currentSystemDefault()
): Long {
    return atStartOfDayIn(timeZone).toEpochMilliseconds()
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
