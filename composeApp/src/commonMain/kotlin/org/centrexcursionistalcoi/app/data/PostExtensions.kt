package org.centrexcursionistalcoi.app.data

import androidx.compose.runtime.Composable
import cea_app.composeapp.generated.resources.*
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.DayOfWeekNames
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import org.centrexcursionistalcoi.app.utils.localizedNames
import org.jetbrains.compose.resources.stringResource

@Composable
fun ReferencedPost.localizedDate(): String {
    val localizedDayOfWeekNames = DayOfWeekNames.localizedNames
    val localizedMonthNames = MonthNames.localizedNames
    val joinDayAndMonth = stringResource(Res.string.date_join_day_and_month)
    val joinMonthAndYear = stringResource(Res.string.date_join_month_and_year)
    val format = LocalDateTime.Format {
        hour()
        char(':')
        minute()
        char(' ')

        dayOfWeek(localizedDayOfWeekNames)
        chars(", ")
        day()
        chars(joinDayAndMonth)
        monthName(localizedMonthNames)
        chars(joinMonthAndYear)
        year()
    }

    return date.toLocalDateTime(TimeZone.currentSystemDefault()).format(format)
}
