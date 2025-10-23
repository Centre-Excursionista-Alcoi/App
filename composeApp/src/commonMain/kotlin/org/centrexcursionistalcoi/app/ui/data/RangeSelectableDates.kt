package org.centrexcursionistalcoi.app.ui.data

import androidx.compose.material3.SelectableDates
import kotlinx.datetime.LocalDate

/**
 * A [SelectableDates] implementation that allows selection of dates within a specified range.
 *
 * @param from The start date of the selectable range. If null, there is no lower bound.
 * @param fromInclusive If true, the 'from' date is selectable; if false, only dates after 'from' are selectable.
 * @param to The end date of the selectable range. If null, there is no upper bound.
 * @param toInclusive If true, the 'to' date is selectable; if false, only dates before 'to' are selectable.
 */
class RangeSelectableDates(
    val from: LocalDate?,
    val fromInclusive: Boolean = true,
    val to: LocalDate?,
    val toInclusive: Boolean = true,
): SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
        val date = LocalDate.fromEpochDays(utcTimeMillis / (24 * 60 * 60 * 1000))
        val afterFrom = when {
            from == null -> true
            fromInclusive -> date >= from
            else -> date > from
        }
        val beforeTo = when {
            to == null -> true
            toInclusive -> date <= to
            else -> date < to
        }
        return afterFrom && beforeTo
    }
}
