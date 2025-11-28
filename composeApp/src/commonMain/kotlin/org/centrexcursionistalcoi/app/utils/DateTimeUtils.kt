package org.centrexcursionistalcoi.app.utils

import androidx.compose.runtime.Composable
import cea_app.composeapp.generated.resources.*
import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.DayOfWeekNames
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource

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
    val localizedDayOfWeekNames = DayOfWeekNames.localizedNames
    val localizedMonthNames = MonthNames.localizedNames
    val format = LocalDateTime.Format {
        hour()
        char(':')
        minute()
        char(' ')

        dayOfWeek(localizedDayOfWeekNames)
        chars(", ")
        day()
        char(' ')
        monthName(localizedMonthNames)
        char(' ')
        year()
    }
    return instant.toLocalDateTime(TimeZone.currentSystemDefault()).format(format)
}
