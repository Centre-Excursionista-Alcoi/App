package org.centrexcursionistalcoi.app.utils

import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import org.centrexcursionistalcoi.app.data.Category
import org.centrexcursionistalcoi.app.data.CategoryPrice
import org.centrexcursionistalcoi.app.data.PriceUnit

object SpacePricing {
    /** Nights spent in a stay from [checkIn] to [checkOut]. 0 for day use. */
    fun nights(checkIn: LocalDate, checkOut: LocalDate): Int = checkIn.daysUntil(checkOut)

    /**
     * Computes the total price of a stay.
     * @param attendees The number of people of each [Category].
     * Categories without a price are free.
     */
    fun compute(
        prices: List<CategoryPrice>,
        attendees: Map<Category, Int>,
        checkIn: LocalDate,
        checkOut: LocalDate,
    ): Double {
        val nights = nights(checkIn, checkOut)
        val days = nights + 1
        return prices.sumOf { price ->
            val count = attendees[price.category] ?: 0
            val units = when (price.unit) {
                PriceUnit.PER_NIGHT -> nights
                PriceUnit.PER_DAY -> days
            }
            count * price.price * units
        }
    }

    /**
     * Whether two stays collide. Nights are `[checkIn, checkOut)`, so a stay ending the day another starts doesn't
     * collide. A day-use stay (no nights) collides with another day-use on the same date, or with a stay whose nights
     * go through that date (`checkIn < date < checkOut`).
     */
    fun conflicts(aIn: LocalDate, aOut: LocalDate, bIn: LocalDate, bOut: LocalDate): Boolean {
        val aDay = aIn == aOut
        val bDay = bIn == bOut
        return when {
            aDay && bDay -> aIn == bIn
            aDay -> bIn < aIn && aIn < bOut
            bDay -> aIn < bIn && bIn < aOut
            else -> aIn < bOut && bIn < aOut
        }
    }
}
