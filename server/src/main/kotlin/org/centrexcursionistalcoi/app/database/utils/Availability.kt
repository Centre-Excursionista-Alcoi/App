@file:Suppress("UnusedReceiverParameter")

package org.centrexcursionistalcoi.app.database.utils

import java.time.LocalDate
import kotlin.io.encoding.ExperimentalEncodingApi
import org.centrexcursionistalcoi.app.data.IBookingD
import org.centrexcursionistalcoi.app.data.ItemD
import org.centrexcursionistalcoi.app.data.SpaceD
import org.centrexcursionistalcoi.app.database.common.BookingEntity
import org.centrexcursionistalcoi.app.database.common.BookingTable
import org.centrexcursionistalcoi.app.database.entity.Item
import org.centrexcursionistalcoi.app.database.entity.Lending
import org.centrexcursionistalcoi.app.database.entity.LendingItem
import org.centrexcursionistalcoi.app.database.entity.Space
import org.centrexcursionistalcoi.app.database.entity.SpaceBooking
import org.centrexcursionistalcoi.app.database.table.LendingItemsTable
import org.centrexcursionistalcoi.app.database.table.LendingsTable
import org.centrexcursionistalcoi.app.database.table.SpaceBookingsTable
import org.centrexcursionistalcoi.app.database.table.SpacesTable
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.and

/**
 * Fetch all lendings that overlap with the given period.
 *
 * If they partially overlap, they are considered to overlap.
 *
 * Filters automatically already returned bookings.
 *
 * **Must be called within a transaction.**
 */
fun <BookingD : IBookingD, Entity : BookingEntity<BookingD>, EntityClass : IntEntityClass<Entity>, Table : BookingTable> Transaction.lendingsForDates(
    from: LocalDate,
    to: LocalDate,
    entityClass: EntityClass,
    table: Table
): List<BookingD> {
    return entityClass.find {
        // (StartA <= EndB) and (EndA >= StartB)
        // Proof: https://stackoverflow.com/a/325964
        (table.from lessEq to) and (table.to greaterEq from) and
            // if already returned, the element can be used again
            (table.returnedAt.isNull())
    }.map { it.serializable() }
}

/**
 * Fetch all items that are available for the given period.
 *
 * **Must be called within a transaction.**
 */
fun Transaction.itemsAvailableForDates(from: LocalDate, to: LocalDate): List<ItemD> {
    // Fetch all items in the database
    val allItems = Item.all().map(Item::serializable)
    // Fetch the existing lendings that overlap with the requested period
    val lendingsIds = lendingsForDates(from, to, Lending, LendingsTable).mapNotNull { it.id }
    // Fetch all the items booked for the requested period
    val usedIds = LendingItem.find { LendingItemsTable.lending inList lendingsIds }
        .map { it.item.id.value }
    // Return all the items in the database that are not used
    return allItems.filter { it.id !in usedIds }
}

/**
 * Fetch all spaces that are available for the given period.
 *
 * **Must be called within a transaction.**
 */
@OptIn(ExperimentalEncodingApi::class)
fun Transaction.spacesAvailableForDates(from: LocalDate, to: LocalDate): List<SpaceD> {
    // Fetch the existing lendings that overlap with the requested period
    val usedSpacesIds = lendingsForDates(from, to, SpaceBooking, SpaceBookingsTable).mapNotNull { it.spaceId }
    // Return all the spaces in the database that are not used
    return Space.find { SpacesTable.id notInList usedSpacesIds }
        .map(Space::serializable)
}
