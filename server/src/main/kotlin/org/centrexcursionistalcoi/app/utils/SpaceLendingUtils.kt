package org.centrexcursionistalcoi.app.utils

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.DatePeriod
import org.centrexcursionistalcoi.app.database.entity.SpaceEntity
import org.centrexcursionistalcoi.app.database.entity.SpaceLendingEntity
import org.centrexcursionistalcoi.app.database.table.SpaceLendings
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import kotlin.time.Instant

/**
 * Whether [space] is closed at some moment of a stay from [checkIn] to [checkOut] (both days included).
 */
fun SpaceEntity.isClosedDuring(checkIn: LocalDate, checkOut: LocalDate, zone: TimeZone = TimeZone.currentSystemDefault()): Boolean =
    isClosedDuring(isClosed, closedSince, closedUntil, checkIn, checkOut, zone)

fun isClosedDuring(
    isClosed: Boolean,
    closedSince: Instant?,
    closedUntil: Instant?,
    checkIn: LocalDate,
    checkOut: LocalDate,
    zone: TimeZone = TimeZone.currentSystemDefault(),
): Boolean {
    if (!isClosed) return false
    val start = checkIn.atStartOfDayIn(zone)
    val end = checkOut.plus(DatePeriod(days = 1)).atStartOfDayIn(zone)
    // The space is closed over [closedSince, closedUntil]. A missing bound is open-ended.
    val startsBeforeEnd = closedSince == null || closedSince < end
    val endsAfterStart = closedUntil == null || closedUntil > start
    return startsBeforeEnd && endsAfterStart
}

/**
 * Whether a lending of [space] from [checkIn] to [checkOut] collides with another one that is not cancelled.
 * @param ignore A lending to ignore, e.g. the one being modified.
 */
context(_: JdbcTransaction)
fun hasSpaceLendingConflict(space: SpaceEntity, checkIn: LocalDate, checkOut: LocalDate, ignore: SpaceLendingEntity? = null): Boolean =
    SpaceLendingEntity
        .find { (SpaceLendings.space eq space.id) and (SpaceLendings.cancelled eq false) and (SpaceLendings.checkOut greaterEq checkIn) }
        .any { other ->
            other.id != ignore?.id && SpacePricing.conflicts(checkIn, checkOut, other.checkIn, other.checkOut)
        }
