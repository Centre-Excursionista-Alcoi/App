package org.centrexcursionistalcoi.app.ui.page.main.management

import androidx.compose.ui.graphics.Color
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus
import kotlinx.datetime.DatePeriod
import org.centrexcursionistalcoi.app.data.Lending
import org.centrexcursionistalcoi.app.data.ReferencedLending
import org.centrexcursionistalcoi.app.data.SpaceLending
import org.centrexcursionistalcoi.app.ui.data.LendingKind
import org.centrexcursionistalcoi.app.ui.data.LendingKindFilter
import org.centrexcursionistalcoi.app.ui.screen.spaces.SpaceLendingStage
import org.centrexcursionistalcoi.app.ui.screen.spaces.stage
import kotlin.uuid.Uuid

/**
 * A lending, of any [kind], as the calendar draws it: a bar from [from] to [to] (both included) in [color].
 */
data class CalendarEntry(
    val id: Uuid,
    val kind: LendingKind,
    val from: LocalDate,
    val to: LocalDate,
    val color: Color,
) {
    /** Every day the bar covers. */
    val dates: List<LocalDate> get() = (0..from.daysUntil(to)).map { from.plus(DatePeriod(days = it)) }
}

fun ReferencedLending.toCalendarEntry() = CalendarEntry(id, LendingKind.ITEMS, from, to, status().barColor())

fun SpaceLending.toCalendarEntry() = CalendarEntry(id, LendingKind.SPACE, checkIn, checkOut, stage.barColor())

/**
 * A row of the list of lendings: one of either kind.
 */
sealed interface LendingListEntry {
    val id: Uuid
    val kind: LendingKind

    /** The first day, for sorting. */
    val from: LocalDate

    /** Whether it still needs something from the managers. Otherwise, it's done (or cancelled). */
    val isActive: Boolean

    data class Item(val lending: ReferencedLending) : LendingListEntry {
        override val id get() = lending.id
        override val kind get() = LendingKind.ITEMS
        override val from get() = lending.from
        override val isActive get() = lending.status().isPending()
    }

    data class Space(val lending: SpaceLending, val spaceName: String) : LendingListEntry {
        override val id get() = lending.id
        override val kind get() = LendingKind.SPACE
        override val from get() = lending.checkIn
        override val isActive get() = lending.stage.isActive()
    }
}

/**
 * Whether the lending in this stage still needs something from the managers: handing the keys over, taking them back,
 * or setting it paid.
 */
fun SpaceLendingStage.isActive(): Boolean = this != SpaceLendingStage.PAID && this != SpaceLendingStage.CANCELLED

/**
 * The lendings of both kinds that [filter] lets through, most recent first.
 * @param spaceNames The name of each space, by id. A lending of a space that isn't known gets an empty one.
 */
fun mergedLendingEntries(
    items: List<ReferencedLending>?,
    spaceLendings: List<SpaceLending>?,
    spaceNames: Map<Uuid, String>,
    filter: LendingKindFilter,
): List<LendingListEntry> {
    val itemEntries = if (filter.includes(LendingKind.ITEMS)) items.orEmpty().map { LendingListEntry.Item(it) } else emptyList()
    val spaceEntries = if (filter.includes(LendingKind.SPACE)) {
        spaceLendings.orEmpty().map { LendingListEntry.Space(it, spaceNames[it.space].orEmpty()) }
    } else {
        emptyList()
    }
    return (itemEntries + spaceEntries).sortedByDescending { it.from }
}

/** The calendar entries of the lendings that [filter] lets through. */
fun calendarEntries(
    items: List<ReferencedLending>?,
    spaceLendings: List<SpaceLending>?,
    filter: LendingKindFilter,
): List<CalendarEntry> {
    val itemEntries = if (filter.includes(LendingKind.ITEMS)) items.orEmpty().map { it.toCalendarEntry() } else emptyList()
    val spaceEntries = if (filter.includes(LendingKind.SPACE)) spaceLendings.orEmpty().map { it.toCalendarEntry() } else emptyList()
    return itemEntries + spaceEntries
}

/** The colors of equipment lendings: blue, purple and warm tones. Spaces use [SpaceLendingStage.barColor]'s greens. */
fun Lending.Status.barColor(): Color = when (this) {
    Lending.Status.REQUESTED -> Color(0xFFF9A825)
    Lending.Status.CONFIRMED -> Color(0xFF1E88E5)
    Lending.Status.TAKEN -> Color(0xFF3949AB)
    Lending.Status.RETURNED -> Color(0xFF8E24AA)
    Lending.Status.MEMORY_SUBMITTED -> Color(0xFF757575)
    Lending.Status.COMPLETE -> Color(0xFFBDBDBD)
}

/** The colors of space lendings: greens, so they can't be mistaken for equipment lendings. */
fun SpaceLendingStage.barColor(): Color = when (this) {
    SpaceLendingStage.CREATED -> Color(0xFF9CCC65)
    SpaceLendingStage.PICKED_UP -> Color(0xFF43A047)
    SpaceLendingStage.RETURNED -> Color(0xFF1B5E20)
    SpaceLendingStage.PAID -> Color(0xFFA5D6A7)
    SpaceLendingStage.CANCELLED -> Color(0xFFCFD8DC)
}
