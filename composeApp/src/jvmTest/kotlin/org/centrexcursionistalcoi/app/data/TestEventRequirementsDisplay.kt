package org.centrexcursionistalcoi.app.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlin.uuid.Uuid

class TestEventRequirementsDisplay {
    private val now = Instant.fromEpochMilliseconds(10_000)
    private val department = Uuid.random()

    private val belay = Qualification(Uuid.random(), department, "Assegurar", null)
    private val sport = Qualification(Uuid.random(), department, "Escalada esportiva", null)
    private val topRope = Qualification(Uuid.random(), department, "Top rope", null)
    private val all = listOf(belay, sport, topRope)

    private fun grant(q: Qualification, expiresAt: Instant? = null) =
        QualificationGrant(q.id, "sub", null, Instant.fromEpochMilliseconds(0), expiresAt)

    private fun event(vararg groups: List<Qualification>) = ReferencedEvent(
        id = Uuid.random(),
        start = now,
        end = null,
        place = "",
        title = "",
        description = null,
        maxPeople = null,
        requiresConfirmation = true,
        requiresInsurance = false,
        department = null,
        image = null,
        userSubList = emptyList(),
        qualificationRequirements = groups.map { group -> group.map { it.id } },
    )

    @Test
    fun heldAt_ignoresExpiredGrants() {
        val grants = listOf(
            grant(belay),
            grant(sport, expiresAt = Instant.fromEpochMilliseconds(20_000)),
            grant(topRope, expiresAt = Instant.fromEpochMilliseconds(5_000)),
        )
        assertEquals(setOf(belay.id, sport.id), grants.heldAt(now))
    }

    @Test
    fun noRequirements_isEmpty() {
        assertEquals(emptyList(), event().requirements(all, emptyList(), now))
    }

    /** Assegurar AND (Escalada esportiva OR Top rope) */
    @Test
    fun eachGroup_isReportedWithItsNames_andWhetherItIsMet() {
        val event = event(listOf(belay), listOf(sport, topRope))

        val requirements = event.requirements(all, listOf(grant(belay), grant(topRope)), now)

        assertEquals(listOf(listOf("Assegurar"), listOf("Escalada esportiva", "Top rope")), requirements.map { it.alternatives })
        assertTrue(requirements.all { it.isMet })
    }

    @Test
    fun aGroup_isMetByAnyOfItsAlternatives_onlyThatGroupIsUnmet() {
        val event = event(listOf(belay), listOf(sport, topRope))

        val requirements = event.requirements(all, listOf(grant(belay)), now)

        assertTrue(requirements[0].isMet)
        assertFalse(requirements[1].isMet)
    }

    @Test
    fun anExpiredGrant_doesNotMeetARequirement() {
        val event = event(listOf(belay))
        val expired = grant(belay, expiresAt = Instant.fromEpochMilliseconds(9_999))

        assertFalse(event.requirements(all, listOf(expired), now).single().isMet)
    }

    @Test
    fun aQualificationNotKnownLocally_hasNoName_butStillCounts() {
        val event = event(listOf(belay))

        // Definitions not synced yet, but the grant is
        val requirements = event.requirements(qualifications = emptyList(), grants = listOf(grant(belay)), now = now)

        assertEquals(listOf(null), requirements.single().alternatives)
        assertTrue(requirements.single().isMet)
    }
}

class TestEventRequirementsEditing {
    private val a = Uuid.random()
    private val b = Uuid.random()
    private val c = Uuid.random()

    @Test
    fun normalized_dropsEmptyGroups_andRepeatedAlternatives() {
        assertEquals(listOf(listOf(a), listOf(b, c)), listOf(listOf(a, a), emptyList(), listOf(b, c, b)).normalizedRequirements())
        assertEquals(emptyList(), listOf(emptyList<Uuid>()).normalizedRequirements())
    }

    @Test
    fun sameRequirements_ignoresOrder_butNotContent() {
        assertTrue(listOf(listOf(a), listOf(b, c)) sameRequirementsAs listOf(listOf(c, b), listOf(a)))
        // an unfinished, empty group makes no difference to what would be saved
        assertTrue(listOf(listOf(a), emptyList()) sameRequirementsAs listOf(listOf(a)))
        assertFalse(listOf(listOf(a), listOf(b)) sameRequirementsAs listOf(listOf(a, b)))
        assertFalse(listOf(listOf(a)) sameRequirementsAs emptyList())
    }
}
