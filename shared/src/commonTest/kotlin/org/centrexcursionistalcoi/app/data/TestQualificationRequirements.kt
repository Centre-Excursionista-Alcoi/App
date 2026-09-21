package org.centrexcursionistalcoi.app.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

class TestQualificationRequirements {
    private val a = Uuid.random()
    private val b = Uuid.random()
    private val c = Uuid.random()

    @Test
    fun noRequirements_isAlwaysSatisfied() {
        assertTrue(emptyList<List<Uuid>>().isSatisfiedBy(emptySet()))
    }

    @Test
    fun singleGroup_isOr() {
        val requirements = listOf(listOf(a, b))
        assertTrue(requirements.isSatisfiedBy(setOf(a)))
        assertTrue(requirements.isSatisfiedBy(setOf(b)))
        assertTrue(requirements.isSatisfiedBy(setOf(a, b)))
        assertFalse(requirements.isSatisfiedBy(setOf(c)))
        assertFalse(requirements.isSatisfiedBy(emptySet()))
    }

    @Test
    fun separateGroups_areAnd() {
        val requirements = listOf(listOf(a), listOf(b))
        assertTrue(requirements.isSatisfiedBy(setOf(a, b)))
        assertFalse(requirements.isSatisfiedBy(setOf(a)))
        assertFalse(requirements.isSatisfiedBy(setOf(b)))
    }

    /** A * (B + C) */
    @Test
    fun aAndBOrC() {
        val requirements = listOf(listOf(a), listOf(b, c))
        assertTrue(requirements.isSatisfiedBy(setOf(a, b)))
        assertTrue(requirements.isSatisfiedBy(setOf(a, c)))
        assertTrue(requirements.isSatisfiedBy(setOf(a, b, c)))
        // (B OR C) alone doesn't make up for the missing A
        assertFalse(requirements.isSatisfiedBy(setOf(b, c)))
        // A alone doesn't satisfy (B OR C)
        assertFalse(requirements.isSatisfiedBy(setOf(a)))
    }

    @Test
    fun unmetRequirements_reportsExactlyTheUnsatisfiedGroups() {
        val requirements = listOf(listOf(a), listOf(b, c))
        assertEquals(listOf(listOf(b, c)), requirements.unmetRequirements(setOf(a)))
        assertEquals(listOf(listOf(a)), requirements.unmetRequirements(setOf(c)))
        assertEquals(requirements, requirements.unmetRequirements(emptySet()))
        assertEquals(emptyList(), requirements.unmetRequirements(setOf(a, c)))
    }

    @Test
    fun emptyGroup_canNeverBeSatisfied() {
        assertFalse(listOf(emptyList<Uuid>()).isSatisfiedBy(setOf(a, b, c)))
    }
}

class TestQualificationGrant {
    private fun grant(expiresAt: kotlin.time.Instant?) = QualificationGrant(Uuid.random(), "sub", null, kotlin.time.Instant.fromEpochMilliseconds(0), expiresAt)

    @Test
    fun grantWithoutExpiry_isAlwaysActive() {
        assertTrue(grant(null).isActiveAt(kotlin.time.Instant.fromEpochMilliseconds(Long.MAX_VALUE / 1_000_000)))
    }

    @Test
    fun grant_isActiveUntilItsExpiry() {
        val expiry = kotlin.time.Instant.fromEpochMilliseconds(10_000)
        assertTrue(grant(expiry).isActiveAt(kotlin.time.Instant.fromEpochMilliseconds(9_999)))
        // Expired exactly at the expiry, matching the server (which only counts expiresAt > now)
        assertFalse(grant(expiry).isActiveAt(expiry))
        assertFalse(grant(expiry).isActiveAt(kotlin.time.Instant.fromEpochMilliseconds(10_001)))
    }
}
