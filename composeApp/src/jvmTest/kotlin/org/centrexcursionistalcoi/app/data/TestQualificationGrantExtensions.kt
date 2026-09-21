package org.centrexcursionistalcoi.app.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

class TestQualificationGrantExtensions {
    private fun grant(expiresAt: Instant?) = QualificationGrant(Uuid.random(), "sub", null, Instant.fromEpochMilliseconds(0), expiresAt)

    @Test
    fun chosenDate_roundTripsThroughTheExpiry_inAnyTimeZone() {
        val date = LocalDate(2027, 1, 5)
        for (zone in listOf("UTC", "Europe/Madrid", "America/Los_Angeles", "Pacific/Auckland").map(TimeZone::of)) {
            assertEquals(date, grant(date.expiryInstant(zone)).lastValidDate(zone), "in $zone")
        }
    }

    @Test
    fun expiry_isTheStartOfTheNextDay_soTheWholeChosenDayCounts() {
        val zone = TimeZone.of("Europe/Madrid")
        val expiry = LocalDate(2027, 1, 5).expiryInstant(zone)
        // 2027-01-06T00:00 in Madrid (CET, UTC+1) is 2027-01-05T23:00Z
        assertEquals(Instant.parse("2027-01-05T23:00:00Z"), expiry)
    }

    @Test
    fun grantWithoutExpiry_hasNoLastValidDate() {
        assertNull(grant(null).lastValidDate())
    }
}
