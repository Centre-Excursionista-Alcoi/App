package org.centrexcursionistalcoi.app.database.entity

import java.util.UUID
import kotlin.uuid.toKotlinUuid
import org.centrexcursionistalcoi.app.data.DepartmentMemberInfo
import org.centrexcursionistalcoi.app.database.table.DepartmentMembers
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction

class DepartmentMemberEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<DepartmentMemberEntity>(DepartmentMembers)

    var department by DepartmentEntity referencedOn DepartmentMembers.departmentId
    var userSub by DepartmentMembers.userSub
    var confirmed by DepartmentMembers.confirmed

    context(_: JdbcTransaction)
    fun toData(): DepartmentMemberInfo = DepartmentMemberInfo(
        userSub = userSub.value,
        departmentId = department.id.value.toKotlinUuid(),
        confirmed = confirmed
    )
}
