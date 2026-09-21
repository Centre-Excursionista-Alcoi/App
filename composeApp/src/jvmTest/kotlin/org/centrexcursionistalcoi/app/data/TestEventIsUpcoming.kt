package org.centrexcursionistalcoi.app.data

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.datetime.TimeZone

class TestEventIsUpcoming {
    private val utc = TimeZone.UTC
    private val madrid = TimeZone.of("Europe/Madrid")

    private fun at(iso: String) = Instant.parse(iso)

    private fun event(start: Instant, end: Instant? = null) = ReferencedEvent(
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

    // ---- No end date: upcoming until the end of the day it starts on ----

    @Test
    fun withoutEnd_aFutureEvent_isUpcoming() {
        assertTrue(event(start = at("2026-10-07T08:00:00Z")).isUpcoming(at("2026-10-05T12:00:00Z"), utc))
    }

    @Test
    fun withoutEnd_anEventLaterToday_isUpcoming() {
        assertTrue(event(start = at("2026-10-05T18:00:00Z")).isUpcoming(at("2026-10-05T12:00:00Z"), utc))
    }

    @Test
    fun withoutEnd_anEventThatStartedEarlierToday_isStillUpcoming() {
        // an outing that set off at 08:00 is still listed at 20:00
        assertTrue(event(start = at("2026-10-05T08:00:00Z")).isUpcoming(at("2026-10-05T20:00:00Z"), utc))
    }

    @Test
    fun withoutEnd_isListedUntilTheLastMomentOfItsDay_thenDrops() {
        val event = event(start = at("2026-10-05T08:00:00Z"))

        assertTrue(event.isUpcoming(at("2026-10-05T23:59:59Z"), utc))
        assertFalse(event.isUpcoming(at("2026-10-06T00:00:00Z"), utc))
    }

    @Test
    fun withoutEnd_anEventFromAPastDay_isNot() {
        assertFalse(event(start = at("2026-10-04T08:00:00Z")).isUpcoming(at("2026-10-05T12:00:00Z"), utc))
    }

    @Test
    fun withoutEnd_theDayIsTheOneInTheGivenTimeZone() {
        // 2026-10-05T22:00Z is already 00:00 on the 6th in Madrid (UTC+2), so its day ends at 2026-10-06T22:00Z there,
        // but at 2026-10-06T00:00Z in UTC
        val event = event(start = at("2026-10-05T22:00:00Z"))
        val now = at("2026-10-06T01:00:00Z")

        assertFalse(event.isUpcoming(now, utc))
        assertTrue(event.isUpcoming(now, madrid))
    }

    // ---- With an end date: upcoming until it ends ----

    @Test
    fun withEnd_aFutureEvent_isUpcoming() {
        assertTrue(event(start = at("2026-10-07T08:00:00Z"), end = at("2026-10-07T18:00:00Z")).isUpcoming(at("2026-10-05T12:00:00Z"), utc))
    }

    @Test
    fun withEnd_anEventInProgress_isUpcoming() {
        assertTrue(event(start = at("2026-10-04T08:00:00Z"), end = at("2026-10-06T18:00:00Z")).isUpcoming(at("2026-10-05T12:00:00Z"), utc))
    }

    @Test
    fun withEnd_anEventThatEnded_isNot() {
        assertFalse(event(start = at("2026-10-04T08:00:00Z"), end = at("2026-10-04T18:00:00Z")).isUpcoming(at("2026-10-05T12:00:00Z"), utc))
    }

    @Test
    fun withEnd_anEventEndingRightNow_isStillUpcoming() {
        val now = at("2026-10-05T12:00:00Z")
        assertTrue(event(start = at("2026-10-05T08:00:00Z"), end = now).isUpcoming(now, utc))
    }

    @Test
    fun withEnd_theEndDateWins_overTheEndOfTheDay() {
        // an event that ended at 10:00 is over, even though its day isn't
        assertFalse(event(start = at("2026-10-05T08:00:00Z"), end = at("2026-10-05T10:00:00Z")).isUpcoming(at("2026-10-05T12:00:00Z"), utc))
    }
}
