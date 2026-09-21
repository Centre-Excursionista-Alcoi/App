package org.centrexcursionistalcoi.app.data

import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * One group of an event's qualification requirements (see [Event.qualificationRequirements]): the user meets it by
 * holding **any** of [alternatives].
 *
 * @property alternatives The names of the group's qualifications, `null` for one that isn't known locally (yet).
 * @property isMet Whether the user holds at least one of them right now.
 */
data class EventRequirement(
    val alternatives: List<String?>,
    val isMet: Boolean,
)

/** The qualifications these grants give at [now]: expired ones don't count. */
fun List<QualificationGrant>.heldAt(now: Instant): Set<Uuid> =
    filter { it.isActiveAt(now) }.map { it.qualificationId }.toSet()

/**
 * This event's requirement groups, each with its qualifications' names and whether a user holding [grants] meets it
 * at [now]. All groups must be met to attend. Empty if the event has no requirements.
 *
 * This is the same rule as [unmetRequirements], which is what the server enforces when confirming assistance, so
 * what's shown as met here is what the server will accept -- as long as the local copy of the grants is current.
 */
fun ReferencedEvent.requirements(
    qualifications: List<Qualification>,
    grants: List<QualificationGrant>,
    now: Instant,
): List<EventRequirement> {
    val names = qualifications.associate { it.id to it.name }
    val unmet = qualificationRequirements.unmetRequirements(grants.heldAt(now))
    return qualificationRequirements.map { group ->
        EventRequirement(
            alternatives = group.map { names[it] },
            isMet = group !in unmet,
        )
    }
}
