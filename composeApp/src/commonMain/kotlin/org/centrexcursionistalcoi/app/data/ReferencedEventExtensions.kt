package org.centrexcursionistalcoi.app.data

import kotlin.time.Instant
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

/**
 * Whether this event is still to come or in progress at [now], which is what the home page lists as upcoming.
 *
 * With an end date, that's until the end. Without one there's no telling how long it lasts, so it counts until the
 * end of the day it starts on in [timeZone] (the device's, by default), so a same-day outing stays listed through
 * the day. The calendar export makes the same assumption.
 */
fun ReferencedEvent.isUpcoming(now: Instant, timeZone: TimeZone = TimeZone.currentSystemDefault()): Boolean =
    end?.let { it >= now } ?: (now < endOfStartDay(timeZone))

/** The start of the day after the one this event starts on, in [timeZone]: the first moment it's certainly over. */
private fun ReferencedEvent.endOfStartDay(timeZone: TimeZone): Instant =
    start.toLocalDateTime(timeZone).date.plus(1, DateTimeUnit.DAY).atStartOfDayIn(timeZone)

/**
 * Whether a member can still confirm or withdraw their assistance at [now]: only until the event starts. The server
 * refuses both once it has (`EventInThePast`), so the app doesn't offer them then.
 */
fun ReferencedEvent.canChangeAssistance(now: Instant): Boolean = start >= now
