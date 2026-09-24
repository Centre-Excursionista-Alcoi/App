package org.centrexcursionistalcoi.app.database.table

import kotlinx.serialization.SerializationStrategy
import org.centrexcursionistalcoi.app.data.DepartmentMemberInfo
import org.centrexcursionistalcoi.app.data.Qualification
import org.centrexcursionistalcoi.app.data.QualificationGrant
import org.centrexcursionistalcoi.app.database.DatabaseNowExpression
import org.centrexcursionistalcoi.app.database.entity.DepartmentEntity
import org.centrexcursionistalcoi.app.database.utils.CustomTableSerializer
import org.centrexcursionistalcoi.app.database.utils.list
import org.centrexcursionistalcoi.app.security.UserSession
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.javatime.timestamp
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import java.util.UUID

object Departments : UUIDTable("departments"), CustomTableSerializer<UUID, DepartmentEntity> {
    val lastUpdate = timestamp("lastUpdate").defaultExpression(DatabaseNowExpression)

    val displayName = varchar("displayName", 255)
    val image = optReference("image", Files, ReferenceOption.SET_NULL)

    override fun columnSerializers(): Map<String, SerializationStrategy<*>> {
        return mapOf(
            "members" to DepartmentMemberInfo.serializer().list(),
            "qualifications" to Qualification.serializer().list(),
            "qualificationGrants" to QualificationGrant.serializer().list(),
        )
    }

    // The department itself (displayName/image) is public, but its member roster is not: it includes each
    // member's sub, roles, and confirmation status (so pending join requests too), matching what
    // GET /departments/{id}/members already restricts by hand -- see DepartmentEntity.visibleMembersFor, the
    // single shared implementation of that rule both routes call.
    //
    // Qualification definitions are public (mirrors GET /qualifications), but grants are not -- see
    // DepartmentEntity.visibleQualificationGrantsFor for that rule, mirroring GET /qualifications/{id}/grants.
    // Embedding both here is what lets a department sync (see SyncAllDataBackgroundJob) carry qualifications
    // like any other referenced data, instead of a separate bulk endpoint/sync step.
    context(_: JdbcTransaction)
    override fun extraColumns(entity: DepartmentEntity, session: UserSession?): Map<String, Any?> {
        return mapOf(
            "members" to entity.visibleMembersFor(session).map { it.toData() },
            "qualifications" to entity.qualifications.map { it.toData() },
            "qualificationGrants" to entity.visibleQualificationGrantsFor(session),
        )
    }
}
