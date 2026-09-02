package org.centrexcursionistalcoi.app.database.table

import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.builtins.serializer
import org.centrexcursionistalcoi.app.data.Sports
import org.centrexcursionistalcoi.app.data.ZonedDateTime
import org.centrexcursionistalcoi.app.database.DatabaseNowExpression
import org.centrexcursionistalcoi.app.database.entity.MemoryEntity
import org.centrexcursionistalcoi.app.database.utils.CustomTableSerializer
import org.centrexcursionistalcoi.app.database.utils.list
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.javatime.timestamp
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import java.util.UUID
import kotlin.uuid.Uuid
import kotlin.uuid.toKotlinUuid

object Memories : UUIDTable("memories"), CustomTableSerializer<UUID, MemoryEntity> {
    val createdAt = timestamp("createdAt").defaultExpression(DatabaseNowExpression)
    val lastUpdate = timestamp("lastUpdate").defaultExpression(DatabaseNowExpression)

    val place = text("place").nullable()
    val externalPeople = text("externalPeople").nullable()
    val text = text("text")
    val sport = enumerationByName("sport", 50, Sports::class).nullable()
    val department = optReference("department", Departments, onDelete = ReferenceOption.SET_NULL)

    // Who submitted the memory. Always set: memories can only be created by a logged-in user.
    val submittedBy = reference("submittedBy", UserReferences, onDelete = ReferenceOption.CASCADE)

    // When the described activity took place. For lending memories this is filled in automatically from the
    // lending's from/to; for standalone memories the client must provide it. Stored as a plain instant plus the IANA
    // zone id it was recorded in, and re-exposed as a combined "from"/"to" `ZonedDateTime` below (see
    // [columnSerializers]/[extraColumns]) rather than as raw columns.
    val fromInstant = timestamp("fromInstant")
    val fromZone = varchar("fromZone", 64)
    val toInstant = timestamp("toInstant")
    val toZone = varchar("toZone", 64)

    val lending = optReference("lending", Lendings, onDelete = ReferenceOption.RESTRICT)
    val pdf = optReference("pdf", Files, onDelete = ReferenceOption.SET_NULL)

    init {
        // There must be only one memory per lending, if the lending is set
        uniqueIndex("memories_lending_unique", lending)

        check("memories_from_is_before_to") { fromInstant lessEq toInstant }
    }

    override fun columnSerializers(): Map<String, SerializationStrategy<*>> = mapOf(
        "members" to UInt.serializer().list(),
        "attachments" to Uuid.serializer().list(),
        "from" to ZonedDateTime.serializer(),
        "to" to ZonedDateTime.serializer(),
    )

    context(_: JdbcTransaction)
    override fun extraColumns(entity: MemoryEntity): Map<String, Any?> = mapOf(
        "members" to entity.members.map { it.memberNumber },
        "attachments" to entity.files.map { it.id.value.toKotlinUuid() },
        "from" to entity.from,
        "to" to entity.to,
    )
}
