package org.centrexcursionistalcoi.app.database.entity

import org.centrexcursionistalcoi.app.database.table.DepartmentMembers
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass

class DepartmentMemberEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<DepartmentMemberEntity>(DepartmentMembers)

    var department by DepartmentEntity referencedOn DepartmentMembers.departmentId
    var userSub by DepartmentMembers.userSub
    var confirmed by DepartmentMembers.confirmed
}
