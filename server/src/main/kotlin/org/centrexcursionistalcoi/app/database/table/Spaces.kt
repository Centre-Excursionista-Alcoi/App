package org.centrexcursionistalcoi.app.database.table

import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.builtins.ListSerializer
import org.centrexcursionistalcoi.app.data.CategoryPrice
import org.centrexcursionistalcoi.app.database.DatabaseNowExpression
import org.centrexcursionistalcoi.app.database.entity.SpaceEntity
import org.centrexcursionistalcoi.app.database.utils.CustomTableSerializer
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.security.UserSession
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.json.jsonb
import kotlin.uuid.Uuid

object Spaces : UuidTable("spaces"), CustomTableSerializer<Uuid, SpaceEntity> {
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

    // Not serialized as a column (it's a list): exposed through [extraColumns]
    val prices = jsonb("prices", json, ListSerializer(CategoryPrice.serializer()))

    override fun columnSerializers(): Map<String, SerializationStrategy<*>> = mapOf(
        "prices" to ListSerializer(CategoryPrice.serializer()),
    )

    context(_: JdbcTransaction)
    override fun extraColumns(entity: SpaceEntity, session: UserSession?): Map<String, Any?> = mapOf(
        "prices" to entity.prices,
    )
}
