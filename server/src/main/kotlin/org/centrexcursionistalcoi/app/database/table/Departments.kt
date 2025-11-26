package org.centrexcursionistalcoi.app.database.table

import java.util.UUID
import kotlinx.serialization.SerializationStrategy
import org.centrexcursionistalcoi.app.data.DepartmentMemberInfo
import org.centrexcursionistalcoi.app.database.entity.DepartmentEntity
import org.centrexcursionistalcoi.app.database.utils.CustomTableSerializer
import org.centrexcursionistalcoi.app.database.utils.list
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.javatime.CurrentTimestamp
import org.jetbrains.exposed.v1.javatime.timestamp
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction

object Departments : UUIDTable("departments"), CustomTableSerializer<UUID, DepartmentEntity> {
    val lastUpdate = timestamp("lastUpdate").defaultExpression(CurrentTimestamp)

    val displayName = varchar("displayName", 255)
    val image = optReference("image", Files, ReferenceOption.SET_NULL)

    override fun columnSerializers(): Map<String, SerializationStrategy<*>> {
        return mapOf("members" to DepartmentMemberInfo.serializer().list())
    }

    context(_: JdbcTransaction)
    override fun extraColumns(entity: DepartmentEntity): Map<String, Any?> {
        return mapOf("members" to entity.members.map { it.toData() })
    }
}
