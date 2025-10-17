package org.centrexcursionistalcoi.app.ui.data

import androidx.compose.material3.SelectableDates
import kotlinx.datetime.LocalDate

/**
 * A [SelectableDates] implementation that allows selection of dates from a given date in the future.
 *
 * @param from The date from which selection is allowed.
 * @param inclusive If true, the 'from' date is selectable; if false, only dates after 'from' are selectable.
 */
class FutureSelectableDates(val from: LocalDate, val inclusive: Boolean = true) : SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
        val date = LocalDate.fromEpochDays(utcTimeMillis / (24 * 60 * 60 * 1000))
        return if (inclusive) {
            date >= from
        } else {
            date > from
        }
    }
}
