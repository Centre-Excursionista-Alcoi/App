package org.centrexcursionistalcoi.app.data

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlin.uuid.Uuid

class TestEventIsUpcoming {
    private val now = Instant.fromEpochMilliseconds(1_000_000)
    private val before = Instant.fromEpochMilliseconds(500_000)
    private val after = Instant.fromEpochMilliseconds(1_500_000)

    private fun event(start: Instant, end: Instant?) = ReferencedEvent(
        id = Uuid.random(),
        start = start,
        end = end,
        place = "",
        title = "",
        description = null,
        maxPeople = null,
        requiresConfirmation = false,
        requiresInsurance = false,
        department = null,
        image = null,
        userSubList = emptyList(),
    )

    // ---- No end date: upcoming until it starts ----

    @Test
    fun withoutEnd_aFutureEvent_isUpcoming() {
        assertTrue(event(start = after, end = null).isUpcoming(now))
    }

    @Test
    fun withoutEnd_aPastEvent_isNot() {
        assertFalse(event(start = before, end = null).isUpcoming(now))
    }

    @Test
    fun withoutEnd_anEventStartingRightNow_isStillUpcoming() {
        assertTrue(event(start = now, end = null).isUpcoming(now))
    }

    // ---- With an end date: upcoming until it ends ----

    @Test
    fun withEnd_aFutureEvent_isUpcoming() {
        assertTrue(event(start = after, end = Instant.fromEpochMilliseconds(2_000_000)).isUpcoming(now))
    }

    @Test
    fun withEnd_anEventInProgress_isUpcoming() {
        assertTrue(event(start = before, end = after).isUpcoming(now))
    }

    @Test
    fun withEnd_anEventThatEnded_isNot() {
        assertFalse(event(start = Instant.fromEpochMilliseconds(100_000), end = before).isUpcoming(now))
    }

    @Test
    fun withEnd_anEventEndingRightNow_isStillUpcoming() {
        assertTrue(event(start = before, end = now).isUpcoming(now))
    }
}
