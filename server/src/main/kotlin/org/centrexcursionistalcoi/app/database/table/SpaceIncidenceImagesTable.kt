package org.centrexcursionistalcoi.app.database.table

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp

object SpaceIncidenceImagesTable : UUIDTable() {
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)

    val image = binary("image").nullable()

    val incidence = reference("incidence_id", SpaceIncidencesTable, onDelete = ReferenceOption.CASCADE)
}
