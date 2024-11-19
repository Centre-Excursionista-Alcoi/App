package org.centrexcursionistalcoi.app.database.utils

import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.io.encoding.ExperimentalEncodingApi
import org.centrexcursionistalcoi.app.data.ItemD
import org.centrexcursionistalcoi.app.data.ItemLendingD
import org.centrexcursionistalcoi.app.data.SpaceBookingD
import org.centrexcursionistalcoi.app.data.SpaceD
import org.centrexcursionistalcoi.app.database.entity.Item
import org.centrexcursionistalcoi.app.database.entity.Lending
import org.centrexcursionistalcoi.app.database.entity.LendingItem
import org.centrexcursionistalcoi.app.database.entity.Space
import org.centrexcursionistalcoi.app.database.entity.SpaceBooking
import org.centrexcursionistalcoi.app.database.table.LendingItemsTable
import org.centrexcursionistalcoi.app.database.table.LendingsTable
import org.centrexcursionistalcoi.app.database.table.SpaceBookingsTable
import org.jetbrains.exposed.sql.and

/**
 * Fetch all item lendings that overlap with the given period.
 *
 * If they partially overlap, they are considered to overlap.
 *
 * **Must be called within a transaction.**
 */
fun itemLendingsForDates(from: LocalDateTime, to: LocalDateTime): List<ItemLendingD> {
    return Lending.find {
        // (StartA <= EndB) and (EndA >= StartB)
        // Proof: https://stackoverflow.com/a/325964
        (LendingsTable.from lessEq to) and (LendingsTable.to greaterEq from) and
            // Only fetch the lendings that are not returned
            (LendingsTable.returnedAt eq null)
    }.map(Lending::serializable)
}

/**
 * Fetch all items that are available for the given period.
 *
 * **Must be called within a transaction.**
 */
fun itemsAvailableForDates(from: LocalDateTime, to: LocalDateTime): List<ItemD> {
    // Fetch all items in the database
    val allItems = Item.all().map(Item::serializable)
    // Fetch the existing lendings that overlap with the requested period
    val lendingsIds = itemLendingsForDates(from, to).mapNotNull { it.id }
    // Fetch all the items booked for the requested period
    val usedItemsIds = LendingItem.find { LendingItemsTable.lending inList lendingsIds }
        .map { it.item.id.value }
    // Return all the items in the database that are not used
    return allItems.filter { it.id !in usedItemsIds }
}


/**
 * Fetch all space lendings that overlap with the given period.
 *
 * If they partially overlap, they are considered to overlap.
 *
 * Filters automatically already paid bookings.
 *
 * **Must be called within a transaction.**
 */
fun spaceLendingsForDates(from: LocalDate, to: LocalDate): List<SpaceBookingD> {
    return SpaceBooking.find {
        // (StartA <= EndB) and (EndA >= StartB)
        // Proof: https://stackoverflow.com/a/325964
        (SpaceBookingsTable.from lessEq to) and (SpaceBookingsTable.to greaterEq from) and
            // Only fetch the lendings that are not returned
            (SpaceBookingsTable.paid eq false)
    }.map(SpaceBooking::serializable)
}

/**
 * Fetch all spaces that are available for the given period.
 *
 * **Must be called within a transaction.**
 */
@OptIn(ExperimentalEncodingApi::class)
fun spacesAvailableForDates(from: LocalDate, to: LocalDate): List<SpaceD> {
    // Fetch all spaces in the database
    val allSpaces = Space.all().map(Space::serializable)
    // Fetch the existing lendings that overlap with the requested period
    val bookingsIds = spaceLendingsForDates(from, to).mapNotNull { it.id }
    // Fetch all the spaces booked for the requested period
    val usedSpacesIds = SpaceBooking.find { SpaceBookingsTable.space inList bookingsIds }
        .map { it.space.id.value }
    // Return all the spaces in the database that are not used
    return allSpaces.filter { it.id !in usedSpacesIds }
}
