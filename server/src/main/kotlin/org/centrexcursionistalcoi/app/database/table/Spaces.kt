package org.centrexcursionistalcoi.app.database.table

import kotlinx.serialization.builtins.ListSerializer
import org.centrexcursionistalcoi.app.data.CategoryPrice
import org.centrexcursionistalcoi.app.database.DatabaseNowExpression
import org.centrexcursionistalcoi.app.json
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.json.jsonb

object Spaces : UuidTable("spaces") {
    // ALL COLUMN NAMES MUST MATCH THE FIELD NAMES
    val lastUpdate = timestamp("lastUpdate").defaultExpression(DatabaseNowExpression)

    val name = text("name")
    val description = text("description")

    val conditionsOfUse = text("conditionsOfUse").nullable()

    val requiresKeys = bool("requiresKeys").default(false)

    val isClosed = bool("isClosed").default(false)
    val closedSince = timestamp("closedSince").nullable()
    val closedUntil = timestamp("closedUntil").nullable()
    val closedReason = text("closedReason").nullable()

    val prices = jsonb("prices", json, ListSerializer(CategoryPrice.serializer()))
}
