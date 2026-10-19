package org.centrexcursionistalcoi.app.ui.page.main.management

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.datetime.LocalDate
import org.centrexcursionistalcoi.app.data.Lending
import org.centrexcursionistalcoi.app.data.PaymentStatus
import org.centrexcursionistalcoi.app.data.ReferencedInventoryItem
import org.centrexcursionistalcoi.app.data.ReferencedInventoryItemType
import org.centrexcursionistalcoi.app.data.ReferencedLending
import org.centrexcursionistalcoi.app.data.SpaceLending
import org.centrexcursionistalcoi.app.data.UserData
import org.centrexcursionistalcoi.app.ui.data.LendingKind
import org.centrexcursionistalcoi.app.ui.data.LendingKindFilter
import org.centrexcursionistalcoi.app.ui.screen.spaces.SpaceLendingStage
import org.centrexcursionistalcoi.app.ui.utils.spaceLendingsCountBadge

class TestMergedLendings {
    private val user = UserData("u", 1u, "Maria Garcia", "m@example.com", emptyList(), emptyList(), null, emptyList(), false)
    private val type = ReferencedInventoryItemType(Uuid.random(), "Rope", null, null, null, null, null)
    private val item = ReferencedInventoryItem(Uuid.random(), null, type, null, null)
    private val spaceId = Uuid.random()

    private fun itemLending(from: LocalDate, to: LocalDate, confirmed: Boolean = true, taken: Boolean = false, returned: Boolean = false, memory: Boolean = false) =
        ReferencedLending(
            id = Uuid.random(), user = user, timestamp = Instant.parse("2025-10-01T10:00:00Z"), confirmed = confirmed,
            taken = taken, givenBy = null, givenAt = null, returned = returned, receivedItems = emptyList(),
            memorySubmitted = memory, memorySubmittedAt = null, memory = null, memoryReviewed = memory,
            from = from, to = to, notes = null, items = listOf(item),
        )

    private fun spaceLending(
        checkIn: LocalDate,
        checkOut: LocalDate,
        cancelled: Boolean = false,
        pickedUp: Boolean = false,
        returned: Boolean = false,
        paid: Boolean = false,
    ) = SpaceLending(
        id = Uuid.random(), timestamp = Instant.parse("2025-10-01T10:00:00Z"), space = spaceId, userSub = user.sub,
        checkIn = checkIn, checkOut = checkOut, attendees = emptyMap(), acceptedConditionsAt = null, cancelled = cancelled,
        notes = null,
        pickedUpAt = Instant.parse("2025-10-02T10:00:00Z").takeIf { pickedUp },
        pickedUpBy = null,
        returnedAt = Instant.parse("2025-10-03T10:00:00Z").takeIf { returned },
        returnedBy = null, requestedKeys = emptyMap(), keys = emptyList(), totalPrice = 10.0,
        paymentStatus = if (paid) PaymentStatus.COMPLETED else PaymentStatus.PENDING,
        reportNotes = null, reportIssues = null, reportSubmittedAt = null,
        reportNotesFiles = emptyList(), reportIssuesFiles = emptyList(), paymentProofs = emptyList(),
    )

    private fun day(n: Int) = LocalDate(2026, 10, n)

    @Test
    fun `both kinds are merged, most recent first`() {
        val oldItem = itemLending(day(1), day(3))
        val newSpace = spaceLending(day(20), day(22))
        val midItem = itemLending(day(10), day(12))

        val merged = mergedLendingEntries(listOf(oldItem, midItem), listOf(newSpace), mapOf(spaceId to "Refugi"), LendingKindFilter.ALL)

        assertEquals(listOf(newSpace.id, midItem.id, oldItem.id), merged.map { it.id })
        assertEquals(listOf(LendingKind.SPACE, LendingKind.ITEMS, LendingKind.ITEMS), merged.map { it.kind })
        assertEquals("Refugi", (merged.first() as LendingListEntry.Space).spaceName)
    }

    @Test
    fun `the filter leaves out the other kind`() {
        val items = listOf(itemLending(day(1), day(2)))
        val spaces = listOf(spaceLending(day(3), day(4)))

        assertEquals(listOf(LendingKind.ITEMS), mergedLendingEntries(items, spaces, emptyMap(), LendingKindFilter.ITEMS).map { it.kind })
        assertEquals(listOf(LendingKind.SPACE), mergedLendingEntries(items, spaces, emptyMap(), LendingKindFilter.SPACES).map { it.kind })
        assertEquals(listOf(LendingKind.ITEMS), calendarEntries(items, spaces, LendingKindFilter.ITEMS).map { it.kind })
        assertEquals(listOf(LendingKind.SPACE), calendarEntries(items, spaces, LendingKindFilter.SPACES).map { it.kind })
        assertEquals(2, calendarEntries(items, spaces, LendingKindFilter.ALL).size)
    }

    @Test
    fun `missing data is treated as empty, and an unknown space has no name`() {
        assertTrue(mergedLendingEntries(null, null, emptyMap(), LendingKindFilter.ALL).isEmpty())
        val merged = mergedLendingEntries(null, listOf(spaceLending(day(1), day(2))), emptyMap(), LendingKindFilter.ALL)
        assertEquals("", (merged.single() as LendingListEntry.Space).spaceName)
    }

    @Test
    fun `what still needs the managers is active`() {
        val active = listOf(
            LendingListEntry.Item(itemLending(day(1), day(2), confirmed = false)),
            LendingListEntry.Item(itemLending(day(1), day(2), taken = true)),
            LendingListEntry.Space(spaceLending(day(1), day(2)), ""),
            LendingListEntry.Space(spaceLending(day(1), day(2), pickedUp = true), ""),
            LendingListEntry.Space(spaceLending(day(1), day(2), pickedUp = true, returned = true), ""),
        )
        val done = listOf(
            LendingListEntry.Item(itemLending(day(1), day(2), taken = true, returned = true, memory = true)),
            LendingListEntry.Space(spaceLending(day(1), day(2), paid = true), ""),
            LendingListEntry.Space(spaceLending(day(1), day(2), cancelled = true), ""),
        )
        assertTrue(active.all { it.isActive }, "active: ${active.filterNot { it.isActive }}")
        assertTrue(done.none { it.isActive }, "done: ${done.filter { it.isActive }}")
    }

    @Test
    fun `the bar covers both the first and the last day`() {
        val entry = spaceLending(day(5), day(7)).toCalendarEntry()
        assertEquals(listOf(day(5), day(6), day(7)), entry.dates)
        assertEquals(listOf(day(5)), itemLending(day(5), day(5)).toCalendarEntry().dates)
    }

    @Test
    fun `overlapping lendings of different kinds get their own rows`() {
        val first = itemLending(day(1), day(5)).toCalendarEntry()
        val overlapping = spaceLending(day(4), day(8)).toCalendarEntry()
        val later = itemLending(day(10), day(12)).toCalendarEntry()

        val drawn = computeLendingsDrawings(listOf(first, overlapping, later)).associate { it.entry.id to it.yIndex }

        assertEquals(0, drawn.getValue(first.id))
        assertEquals(1, drawn.getValue(overlapping.id))
        // Nothing overlaps it, so it goes back to the first row
        assertEquals(0, drawn.getValue(later.id))
    }

    @Test
    fun `only the space lendings that need the managers are counted`() {
        assertNull(null.spaceLendingsCountBadge())
        assertNull(listOf(spaceLending(day(1), day(2), paid = true), spaceLending(day(1), day(2), cancelled = true)).spaceLendingsCountBadge())
        val mixed = listOf(
            spaceLending(day(1), day(2)),
            spaceLending(day(1), day(2), pickedUp = true),
            spaceLending(day(1), day(2), paid = true),
        )
        assertEquals(2, mixed.spaceLendingsCountBadge())
    }

    @Test
    fun `equipment and space lendings never share a color`() {
        val items = Lending.Status.entries.map { it.barColor() }.toSet()
        val spaces = SpaceLendingStage.entries.map { it.barColor() }.toSet()
        assertEquals(Lending.Status.entries.size, items.size)
        assertEquals(SpaceLendingStage.entries.size, spaces.size)
        assertTrue(items.intersect(spaces).isEmpty())
    }
}
