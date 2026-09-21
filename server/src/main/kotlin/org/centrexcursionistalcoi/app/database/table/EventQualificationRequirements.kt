package org.centrexcursionistalcoi.app.database.table

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table

/**
 * The qualifications an event requires to confirm assistance, see
 * [org.centrexcursionistalcoi.app.data.Event.qualificationRequirements].
 *
 * Rows sharing an [event] and [groupIndex] are alternatives (OR); an event's distinct groups must all be
 * satisfied (AND).
 */
object EventQualificationRequirements : Table("event_qualification_requirements") {
    val event = reference("event_id", Events, ReferenceOption.CASCADE, ReferenceOption.RESTRICT)
    val groupIndex = integer("group_index")

    // A qualification that's still required by an event can't be deleted out from under it
    val qualification = reference("qualification_id", Qualifications, ReferenceOption.RESTRICT, ReferenceOption.RESTRICT)

    override val primaryKey = PrimaryKey(event, groupIndex, qualification, name = "PK_EventQualificationRequirements")
}
