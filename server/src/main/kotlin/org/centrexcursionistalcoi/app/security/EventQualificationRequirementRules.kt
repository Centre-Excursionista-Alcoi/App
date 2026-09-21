package org.centrexcursionistalcoi.app.security

import java.util.UUID
import org.centrexcursionistalcoi.app.database.entity.QualificationEntity
import org.centrexcursionistalcoi.app.database.table.Qualifications
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction

/** Thrown by [validatedQualificationRequirements] when the requested requirements can't be stored as they are. */
class InvalidQualificationRequirementsException(message: String) : IllegalArgumentException(message)

/**
 * Checks [groups] (see [org.centrexcursionistalcoi.app.data.Event.qualificationRequirements]) as the
 * requirements of an event that belongs to [departmentId], and returns them normalized: duplicate qualifications
 * within a group and duplicate groups are dropped.
 *
 * An event may only require qualifications of its own department, so an event without a department can't have
 * any requirements, and every qualification must exist and belong to [departmentId].
 *
 * @throws InvalidQualificationRequirementsException if the requirements are not valid for the event.
 */
context(_: JdbcTransaction)
fun validatedQualificationRequirements(departmentId: UUID?, groups: List<List<UUID>>): List<List<UUID>> {
    if (groups.isEmpty()) return emptyList()

    if (departmentId == null) {
        throw InvalidQualificationRequirementsException("An event without a department cannot require qualifications.")
    }
    if (groups.any { it.isEmpty() }) {
        throw InvalidQualificationRequirementsException("A requirement group must contain at least one qualification.")
    }

    val normalized = groups.map { it.distinct() }.distinctBy { it.toSet() }

    val requested = normalized.flatten().toSet()
    val found = QualificationEntity.find { Qualifications.id inList requested }.toList()
    val missing = requested - found.map { it.id.value }.toSet()
    if (missing.isNotEmpty()) {
        throw InvalidQualificationRequirementsException("Unknown qualification(s): ${missing.joinToString()}.")
    }
    val foreign = found.filter { it.department.id.value != departmentId }
    if (foreign.isNotEmpty()) {
        throw InvalidQualificationRequirementsException(
            "An event can only require qualifications of its own department. Foreign qualification(s): ${foreign.joinToString { it.id.value.toString() }}."
        )
    }

    return normalized
}
