package org.centrexcursionistalcoi.app.database.utils

import java.time.LocalDateTime
import org.centrexcursionistalcoi.app.database.entity.Item
import org.centrexcursionistalcoi.app.database.entity.Lending
import org.centrexcursionistalcoi.app.database.entity.LendingItem
import org.centrexcursionistalcoi.app.database.table.LendingItemsTable
import org.centrexcursionistalcoi.app.database.table.LendingsTable
import org.centrexcursionistalcoi.app.server.response.data.ItemD
import org.centrexcursionistalcoi.app.server.response.data.LendingD
import org.jetbrains.exposed.sql.and

/**
 * Fetch all lendings that overlap with the given period.
 *
 * If they partially overlap, they are considered to overlap.
 *
 * **Must be called within a transaction.**
 */
fun lendingsForDates(from: LocalDateTime, to: LocalDateTime): List<LendingD> {
    return Lending.find {
        // (StartA <= EndB) and (EndA >= StartB)
        // Proof: https://stackoverflow.com/a/325964
        (LendingsTable.from lessEq to) and (LendingsTable.to greaterEq from)
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
    val lendingsIds = lendingsForDates(from, to).mapNotNull { it.id }
    // Fetch all the items booked for the requested period
    val usedItemsIds = LendingItem.find { LendingItemsTable.lending inList lendingsIds }
        .map { it.item.id.value }
    // Return all the items in the database that are not used
    return allItems.filter { it.id !in usedItemsIds }
}
