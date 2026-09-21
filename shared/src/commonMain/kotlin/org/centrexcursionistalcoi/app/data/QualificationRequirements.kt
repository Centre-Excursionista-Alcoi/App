package org.centrexcursionistalcoi.app.data

import kotlin.uuid.Uuid

/**
 * The requirement groups (see [Event.qualificationRequirements]) not satisfied by a user holding [held]: a group
 * is satisfied when at least one of its qualifications is held. An empty result means the user is eligible.
 *
 * [held] must only contain qualifications that are currently valid (granted and not expired). A group with no
 * qualifications can never be satisfied, so it is always reported as unmet; the server never stores one.
 */
fun List<List<Uuid>>.unmetRequirements(held: Set<Uuid>): List<List<Uuid>> =
    filter { group -> group.none { it in held } }

/** `true` if a user holding [held] satisfies every requirement group. See [unmetRequirements]. */
fun List<List<Uuid>>.isSatisfiedBy(held: Set<Uuid>): Boolean = unmetRequirements(held).isEmpty()
