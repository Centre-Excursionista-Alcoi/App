package org.centrexcursionistalcoi.app.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.uuid.Uuid
import kotlinx.datetime.LocalDate

class TestUserInsurance {
    private val today = LocalDate(2026, 9, 26)

    private fun insurance(policyNumber: String, validFrom: LocalDate, validTo: LocalDate) = UserInsurance(
        id = Uuid.random(),
        userSub = "sub",
        insuranceCompany = "Company",
        policyNumber = policyNumber,
        validFrom = validFrom,
        validTo = validTo,
        documentId = null,
    )

    @Test
    fun status_boundariesAreInclusive() {
        assertEquals(UserInsurance.Status.ACTIVE, insurance("a", today, today).status(today))
        assertEquals(UserInsurance.Status.UPCOMING, insurance("a", LocalDate(2026, 9, 27), LocalDate(2027, 9, 27)).status(today))
        assertEquals(UserInsurance.Status.EXPIRED, insurance("a", LocalDate(2025, 9, 25), LocalDate(2026, 9, 25)).status(today))
    }

    @Test
    fun sortedForDisplay_activeThenUpcomingThenExpired() {
        val expiredLongAgo = insurance("expired-long-ago", LocalDate(2024, 1, 1), LocalDate(2024, 12, 31))
        val expiredRecently = insurance("expired-recently", LocalDate(2025, 1, 1), LocalDate(2025, 12, 31))
        val activeLong = insurance("active-long", LocalDate(2026, 1, 1), LocalDate(2027, 6, 30))
        val activeShort = insurance("active-short", LocalDate(2026, 1, 1), LocalDate(2026, 12, 31))
        val upcomingLater = insurance("upcoming-later", LocalDate(2027, 6, 1), LocalDate(2027, 12, 31))
        val upcomingSooner = insurance("upcoming-sooner", LocalDate(2027, 1, 1), LocalDate(2027, 12, 31))

        val sorted = listOf(expiredLongAgo, upcomingLater, activeLong, expiredRecently, upcomingSooner, activeShort)
            .sortedForDisplay(today)

        assertEquals(
            listOf("active-short", "active-long", "upcoming-sooner", "upcoming-later", "expired-recently", "expired-long-ago"),
            sorted.map { it.policyNumber },
        )
    }
}
