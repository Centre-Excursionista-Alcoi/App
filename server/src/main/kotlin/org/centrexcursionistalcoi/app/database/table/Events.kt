package org.centrexcursionistalcoi.app.database.table

import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.builtins.serializer
import org.centrexcursionistalcoi.app.database.DatabaseNowExpression
import org.centrexcursionistalcoi.app.database.entity.EventEntity
import org.centrexcursionistalcoi.app.database.utils.CustomTableSerializer
import org.centrexcursionistalcoi.app.database.utils.list
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.javatime.timestamp
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import java.util.*

object Events : UUIDTable("events"), CustomTableSerializer<UUID, EventEntity> {
    val created = timestamp("created").defaultExpression(DatabaseNowExpression)
    val lastUpdate = timestamp("lastUpdate").defaultExpression(DatabaseNowExpression)

    val start = timestamp("start")
    val end = timestamp("end").nullable()

    val place = text("place")

    val title = text("title")
    val description = text("description").nullable()

    val maxPeople = long("maxPeople").nullable()
    val requiresConfirmation = bool("requiresConfirmation").default(false)
    val requiresInsurance = bool("requiresInsurance").default(false)

    val department = optReference("department", Departments)
    val image = optReference("image", Files)


    override fun columnSerializers(): Map<String, SerializationStrategy<*>> = mapOf(
        "userSubList" to String.serializer().list()
    )

    context(_: JdbcTransaction)
    override fun extraColumns(entity: EventEntity): Map<String, Any?> = mapOf(
        "userSubList" to entity.userReferences.map { it.id.value }
    )
}
