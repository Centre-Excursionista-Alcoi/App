package org.centrexcursionistalcoi.app.utils

import java.time.LocalDate
import org.centrexcursionistalcoi.app.database.entity.InventoryItemEntity
import org.centrexcursionistalcoi.app.database.entity.LendingEntity
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.SizedIterable

object LendingUtils {
    /**
     * Checks whether any lending in the list conflicts with the given date range and items.
     * Date ranges are inclusive.
     */
    context(_: JdbcTransaction)
    fun SizedIterable<LendingEntity>.conflictsWith(
        from: LocalDate,
        to: LocalDate,
        items: List<InventoryItemEntity>
    ): Boolean {
        val itemIds = items.map { it.id.value }.toSet()

        return any { lending ->
            // Check for date overlap
            val lendingFrom = lending.from
            val lendingTo = lending.to

            val datesOverlap = !to.isBefore(lendingFrom) && !from.isAfter(lendingTo)
            if (!datesOverlap) return@any false

            // Check for item overlap
            val lendingItemIds = lending.items.map { it.id.value }.toSet()
            lendingItemIds.intersect(itemIds).isNotEmpty()
        }
    }
}
