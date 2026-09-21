package org.centrexcursionistalcoi.app.data

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlin.uuid.Uuid

/** Confirming and withdrawing are only possible until the event starts: the server answers `EventInThePast` after. */
class TestEventCanChangeAssistance {
    private val start = Instant.parse("2026-10-05T12:00:00Z")

    private fun event(end: Instant? = null) = ReferencedEvent(
        id = Uuid.random(),
        start = start,
        end = end,
        place = "",
        title = "",
        description = null,
        maxPeople = null,
        requiresConfirmation = true,
        requiresInsurance = false,
        department = null,
        image = null,
        userSubList = emptyList(),
    )

    @Test
    fun beforeTheStart_itCanChange() {
        assertTrue(event().canChangeAssistance(Instant.parse("2026-10-05T11:59:59Z")))
    }

    @Test
    fun atTheStart_itCanStillChange_asTheServerOnlyRefusesAfter() {
        assertTrue(event().canChangeAssistance(start))
    }

    @Test
    fun afterTheStart_itCannot() {
        assertFalse(event().canChangeAssistance(Instant.parse("2026-10-05T12:00:01Z")))
    }

    @Test
    fun anEventInProgressCannot_evenWithAnEndDateAhead() {
        assertFalse(event(end = Instant.parse("2026-10-07T18:00:00Z")).canChangeAssistance(Instant.parse("2026-10-06T09:00:00Z")))
    }
}
