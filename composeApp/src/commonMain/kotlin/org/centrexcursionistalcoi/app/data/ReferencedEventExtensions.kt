package org.centrexcursionistalcoi.app.data

import kotlin.time.Instant

/**
 * Whether this event is still to come or in progress at [now], which is what the home page lists as upcoming.
 *
 * With an end date, that's until the end. Without one there's no telling how long it lasts, so it counts until it
 * starts: the same as the server, which only shows a member events that haven't started yet.
 */
fun ReferencedEvent.isUpcoming(now: Instant): Boolean = end?.let { it >= now } ?: (start >= now)
