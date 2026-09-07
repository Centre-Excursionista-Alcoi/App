package org.centrexcursionistalcoi.app.ui.data

import androidx.compose.material3.SelectableDates
import com.kizitonwose.calendar.core.now
import kotlinx.datetime.LocalDate

/**
 * A [SelectableDates] implementation that allows selection of dates up to a given date in the past.
 *
 * @param until The date until which selection is allowed.
 * @param inclusive If true, the 'until' date is selectable; if false, only dates before 'until' are selectable.
 */
class PastSelectableDates(val until: LocalDate, val inclusive: Boolean = true) : SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
        val date = LocalDate.fromEpochDays(utcTimeMillis / (24 * 60 * 60 * 1000))
        return if (inclusive) {
            date <= until
        } else {
            date < until
        }
    }

    companion object {
        fun today(): PastSelectableDates {
            val today = LocalDate.now()
            return PastSelectableDates(today, inclusive = true)
        }
    }
}
