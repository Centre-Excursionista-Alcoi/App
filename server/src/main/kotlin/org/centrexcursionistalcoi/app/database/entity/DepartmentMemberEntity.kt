package org.centrexcursionistalcoi.app.database.entity

import org.centrexcursionistalcoi.app.data.DepartmentMemberInfo
import org.centrexcursionistalcoi.app.database.table.DepartmentMembers
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction

class DepartmentMemberEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<DepartmentMemberEntity>(DepartmentMembers)

    var department by DepartmentEntity referencedOn DepartmentMembers.departmentId
    var userSub by DepartmentMembers.userSub
    var confirmed by DepartmentMembers.confirmed

    context(_: JdbcTransaction)
    fun toData(): DepartmentMemberInfo = DepartmentMemberInfo(
        userSub = userSub.value,
        departmentId = department.id.value,
        confirmed = confirmed
    )
}
