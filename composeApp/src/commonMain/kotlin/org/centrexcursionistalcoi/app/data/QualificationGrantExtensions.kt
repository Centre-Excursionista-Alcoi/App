package org.centrexcursionistalcoi.app.data

import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

/**
 * The instant a grant that is valid *through* this date should expire at: the start of the following day in [timeZone],
 * so the whole chosen day still counts.
 */
fun LocalDate.expiryInstant(timeZone: TimeZone = TimeZone.currentSystemDefault()): Instant =
    plus(1, DateTimeUnit.DAY).atStartOfDayIn(timeZone)

/**
 * The last day this grant is valid, in [timeZone], or `null` if it doesn't expire. Inverse of [expiryInstant].
 */
fun QualificationGrant.lastValidDate(timeZone: TimeZone = TimeZone.currentSystemDefault()): LocalDate? =
    expiresAt?.minus(1.milliseconds)?.toLocalDateTime(timeZone)?.date
