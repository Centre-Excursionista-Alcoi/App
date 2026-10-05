package org.centrexcursionistalcoi.app.utils

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.centrexcursionistalcoi.app.data.Category
import org.centrexcursionistalcoi.app.data.CategoryPrice
import org.centrexcursionistalcoi.app.data.PriceUnit

class TestSpacePricing {
    private val fri = LocalDate(2026, 10, 9)
    private val sat = LocalDate(2026, 10, 10)
    private val sun = LocalDate(2026, 10, 11)
    private val mon = LocalDate(2026, 10, 12)

    @Test
    fun test_nights_only_price_dayUseIsFree() {
        val prices = listOf(CategoryPrice(Category.MEMBER, 3.0, PriceUnit.PER_NIGHT))
        val attendees = mapOf(Category.MEMBER to 4)
        assertEquals(0.0, SpacePricing.compute(prices, attendees, sat, sat))
        assertEquals(12.0, SpacePricing.compute(prices, attendees, fri, sat))
        assertEquals(24.0, SpacePricing.compute(prices, attendees, fri, sun))
    }

    @Test
    fun test_daily_price_countsDaysTouched() {
        val prices = listOf(CategoryPrice(Category.NON_MEMBER, 6.0, PriceUnit.PER_DAY))
        val attendees = mapOf(Category.NON_MEMBER to 2)
        assertEquals(12.0, SpacePricing.compute(prices, attendees, sat, sat))
        assertEquals(24.0, SpacePricing.compute(prices, attendees, fri, sat))
    }

    @Test
    fun test_mixed_categories_and_unpricedCategoriesAreFree() {
        val prices = listOf(
            CategoryPrice(Category.MEMBER, 3.0, PriceUnit.PER_NIGHT),
            CategoryPrice(Category.NON_MEMBER, 6.0, PriceUnit.PER_NIGHT),
        )
        val attendees = mapOf(Category.MEMBER to 2, Category.NON_MEMBER to 1, Category.CHILD_MEMBER to 3)
        assertEquals(2 * 3.0 * 2 + 6.0 * 2, SpacePricing.compute(prices, attendees, fri, sun))
    }

    @Test
    fun test_conflicts_adjacentStaysDoNotCollide() {
        // Friday to Saturday morning, and Saturday afternoon to Sunday morning
        assertFalse(SpacePricing.conflicts(fri, sat, sat, sun))
        assertFalse(SpacePricing.conflicts(sat, sun, fri, sat))
    }

    @Test
    fun test_conflicts_overlappingNights() {
        assertTrue(SpacePricing.conflicts(fri, sun, sat, mon))
        assertTrue(SpacePricing.conflicts(fri, sun, fri, sat))
        assertTrue(SpacePricing.conflicts(fri, mon, sat, sun))
    }

    @Test
    fun test_conflicts_dayUse() {
        // Same date day uses collide
        assertTrue(SpacePricing.conflicts(sat, sat, sat, sat))
        assertFalse(SpacePricing.conflicts(sat, sat, fri, fri))
        // Day use in the middle of a stay collides, on its boundaries it doesn't
        assertTrue(SpacePricing.conflicts(sat, sat, fri, sun))
        assertTrue(SpacePricing.conflicts(fri, sun, sat, sat))
        assertFalse(SpacePricing.conflicts(sat, sat, fri, sat))
        assertFalse(SpacePricing.conflicts(sat, sat, sat, sun))
    }

    @Test
    fun test_isClosedDuring() {
        val zone = TimeZone.UTC
        val since = sat.atStartOfDayIn(zone)
        val until = sun.atStartOfDayIn(zone)
        assertFalse(isClosedDuring(false, since, until, fri, mon, zone))
        assertTrue(isClosedDuring(true, since, until, fri, sat, zone))
        assertTrue(isClosedDuring(true, since, null, mon, mon, zone))
        assertTrue(isClosedDuring(true, null, null, fri, fri, zone))
        assertFalse(isClosedDuring(true, since, until, fri, fri, zone))
        assertFalse(isClosedDuring(true, since, until, sun, mon, zone))
    }
}
