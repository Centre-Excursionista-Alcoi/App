package org.centrexcursionistalcoi.app.database.table

import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.builtins.serializer
import org.centrexcursionistalcoi.app.data.Sports
import org.centrexcursionistalcoi.app.database.DatabaseNowExpression
import org.centrexcursionistalcoi.app.database.entity.MemoryEntity
import org.centrexcursionistalcoi.app.database.utils.CustomTableSerializer
import org.centrexcursionistalcoi.app.database.utils.list
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
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

    val lending = optReference("lending", Lendings, onDelete = ReferenceOption.RESTRICT)
    val pdf = optReference("pdf", Files, onDelete = ReferenceOption.SET_NULL)

    init {
        // There must be only one memory per lending, if the lending is set
        uniqueIndex("memories_lending_unique", lending)
    }

    override fun columnSerializers(): Map<String, SerializationStrategy<*>> = mapOf(
        "members" to UInt.serializer().list(),
        "attachments" to Uuid.serializer().list(),
    )

    context(_: JdbcTransaction)
    override fun extraColumns(entity: MemoryEntity): Map<String, Any?> = mapOf(
        "members" to entity.members.map { it.memberNumber },
        "attachments" to entity.files.map { it.id.value.toKotlinUuid() },
    )
}
