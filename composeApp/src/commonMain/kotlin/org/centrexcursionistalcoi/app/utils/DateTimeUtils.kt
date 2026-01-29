package org.centrexcursionistalcoi.app.utils

import androidx.compose.runtime.Composable
import cea_app.composeapp.generated.resources.Res
import cea_app.composeapp.generated.resources.date_with_time
import cea_app.composeapp.generated.resources.date_with_time_range
import cea_app.composeapp.generated.resources.days_of_week_friday
import cea_app.composeapp.generated.resources.days_of_week_monday
import cea_app.composeapp.generated.resources.days_of_week_saturday
import cea_app.composeapp.generated.resources.days_of_week_sunday
import cea_app.composeapp.generated.resources.days_of_week_thursday
import cea_app.composeapp.generated.resources.days_of_week_tuesday
import cea_app.composeapp.generated.resources.days_of_week_wednesday
import cea_app.composeapp.generated.resources.months_april
import cea_app.composeapp.generated.resources.months_august
import cea_app.composeapp.generated.resources.months_december
import cea_app.composeapp.generated.resources.months_february
import cea_app.composeapp.generated.resources.months_january
import cea_app.composeapp.generated.resources.months_july
import cea_app.composeapp.generated.resources.months_june
import cea_app.composeapp.generated.resources.months_march
import cea_app.composeapp.generated.resources.months_may
import cea_app.composeapp.generated.resources.months_november
import cea_app.composeapp.generated.resources.months_october
import cea_app.composeapp.generated.resources.months_september
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.DayOfWeekNames
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Instant

fun LocalTime.withoutSeconds(): LocalTime {
    return LocalTime(hour, minute)
}

val DayOfWeekNames.Companion.localizedNames: DayOfWeekNames
    @Composable
    get() =  DayOfWeekNames(
        stringResource(Res.string.days_of_week_monday),
        stringResource(Res.string.days_of_week_tuesday),
        stringResource(Res.string.days_of_week_wednesday),
        stringResource(Res.string.days_of_week_thursday),
        stringResource(Res.string.days_of_week_friday),
        stringResource(Res.string.days_of_week_saturday),
        stringResource(Res.string.days_of_week_sunday),
    )

val MonthNames.Companion.localizedNames: MonthNames
    @Composable
    get() = MonthNames(
        stringResource(Res.string.months_january),
        stringResource(Res.string.months_february),
        stringResource(Res.string.months_march),
        stringResource(Res.string.months_april),
        stringResource(Res.string.months_may),
        stringResource(Res.string.months_june),
        stringResource(Res.string.months_july),
        stringResource(Res.string.months_august),
        stringResource(Res.string.months_september),
        stringResource(Res.string.months_october),
        stringResource(Res.string.months_november),
        stringResource(Res.string.months_december),
    )

@Composable
fun localizedInstantAsDateTime(instant: Instant): String {
    return localizedDateTime(instant.toLocalDateTime())
}

@Composable
fun localizedLocalDate(date: LocalDate): String {
    val localizedDayOfWeekNames = DayOfWeekNames.localizedNames
    val localizedMonthNames = MonthNames.localizedNames
    val format = LocalDate.Format {
        dayOfWeek(localizedDayOfWeekNames)
        chars(", ")
        day()
        char(' ')
        monthName(localizedMonthNames)
        char(' ')
        year()
    }
    return date.format(format)
}

fun localizedLocalTime(time: LocalTime): String {
    val format = LocalTime.Format {
        hour()
        char(':')
        minute()
    }
    return time.format(format)
}

@Composable
fun localizedDateWithTime(date: LocalDate, time: LocalTime): String {
    val localizedDate = localizedLocalDate(date)
    val localizedTime = localizedLocalTime(time)
    return stringResource(Res.string.date_with_time, localizedDate, localizedTime)
}

@Composable
fun localizedDateTime(dateTime: LocalDateTime): String {
    return localizedDateWithTime(dateTime.date, dateTime.time)
}

@Composable
fun localizedDateWithTimeRange(date: LocalDate, fromTime: LocalTime, toTime: LocalTime): String {
    val localizedDate = localizedLocalDate(date)
    return stringResource(Res.string.date_with_time_range, localizedDate, fromTime.toString(), toTime.toString())
}

/**
 * Returns true if this [Instant] occurs on the same calendar day as [other] in the specified [timeZone].
 */
fun Instant.isSameDayAs(other: Instant, timeZone: TimeZone = TimeZone.currentSystemDefault()): Boolean {
    val thisDateTime = this.toLocalDateTime(timeZone)
    val otherDateTime = other.toLocalDateTime(timeZone)
    return thisDateTime.year == otherDateTime.year &&
        thisDateTime.month == otherDateTime.month &&
        thisDateTime.day == otherDateTime.day
}

/**
 * Converts this [Instant] to a [LocalDateTime] in the system's current default time zone.
 */
fun Instant.toLocalDateTime() = this.toLocalDateTime(TimeZone.currentSystemDefault())
