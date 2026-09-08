package org.centrexcursionistalcoi.app.database.table

import java.util.UUID
import kotlinx.serialization.SerializationStrategy
import org.centrexcursionistalcoi.app.data.DepartmentMemberInfo
import org.centrexcursionistalcoi.app.database.DatabaseNowExpression
import org.centrexcursionistalcoi.app.database.entity.DepartmentEntity
import org.centrexcursionistalcoi.app.database.utils.CustomTableSerializer
import org.centrexcursionistalcoi.app.database.utils.list
import org.centrexcursionistalcoi.app.plugins.UserSession
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.javatime.timestamp
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction

object Departments : UUIDTable("departments"), CustomTableSerializer<UUID, DepartmentEntity> {
    val lastUpdate = timestamp("lastUpdate").defaultExpression(DatabaseNowExpression)

    val displayName = varchar("displayName", 255)
    val image = optReference("image", Files, ReferenceOption.SET_NULL)

    override fun columnSerializers(): Map<String, SerializationStrategy<*>> {
        return mapOf("members" to DepartmentMemberInfo.serializer().list())
    }

    // The department itself (displayName/image) is public, but its member roster is not: it includes each
    // member's sub, roles, and confirmation status (so pending join requests too), matching what
    // GET /departments/{id}/members already restricts by hand -- see DepartmentEntity.visibleMembersFor, the
    // single shared implementation of that rule both routes call.
    context(_: JdbcTransaction)
    override fun extraColumns(entity: DepartmentEntity, session: UserSession?): Map<String, Any?> {
        return mapOf("members" to entity.visibleMembersFor(session).map { it.toData() })
    }
}
