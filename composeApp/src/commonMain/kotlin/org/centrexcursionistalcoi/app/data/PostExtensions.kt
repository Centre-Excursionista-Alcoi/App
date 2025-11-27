package org.centrexcursionistalcoi.app.data

import androidx.compose.runtime.Composable
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.DayOfWeekNames
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import org.centrexcursionistalcoi.app.utils.localizedNames

@Composable
fun ReferencedPost.localizedDate(): String {
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

    return date.toLocalDateTime(TimeZone.currentSystemDefault()).format(format)
}
