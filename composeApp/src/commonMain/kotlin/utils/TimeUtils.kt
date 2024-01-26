package utils

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

const val SecondsInADay = 24 * 60 * 60L

/**
 * Converts the given date to an [Instant].
 */
fun LocalDate.toInstant(): Instant {
    return Instant.fromEpochSeconds(toEpochDays() * SecondsInADay)
}

/**
 * Converts the given instant into the local date of the desired [timeZone].
 */
fun Instant.toLocalDate(timeZone: TimeZone = TimeZone.currentSystemDefault()): LocalDate {
    val dateTime = toLocalDateTime(timeZone)
    return LocalDate(dateTime.year, dateTime.monthNumber, dateTime.dayOfMonth)
}
