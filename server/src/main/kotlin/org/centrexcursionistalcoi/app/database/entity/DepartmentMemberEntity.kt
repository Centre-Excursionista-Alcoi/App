package org.centrexcursionistalcoi.app.database.entity

import java.util.UUID
import kotlin.uuid.toKotlinUuid
import org.centrexcursionistalcoi.app.data.DepartmentMemberInfo
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.table.DepartmentMembers
import org.centrexcursionistalcoi.app.push.PushNotification
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction

class DepartmentMemberEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<DepartmentMemberEntity>(DepartmentMembers) {
        context(_: JdbcTransaction)
        fun getUserDepartments(userSub: String, isConfirmed: Boolean = true) =
            find { (DepartmentMembers.userSub eq userSub) and (DepartmentMembers.confirmed eq isConfirmed) }
                .toList()
    }

    var department by DepartmentEntity referencedOn DepartmentMembers.departmentId
    var userReference by UserReferenceEntity referencedOn DepartmentMembers.userSub
    var confirmed by DepartmentMembers.confirmed
    var isManager by DepartmentMembers.isManager

    context(_: JdbcTransaction)
    fun toData(): DepartmentMemberInfo = DepartmentMemberInfo(
        id = id.value.toKotlinUuid(),
        userSub = userReference.id.value,
        departmentId = department.id.value.toKotlinUuid(),
        confirmed = confirmed,
        isManager = isManager,
    )

    fun confirmedNotification() = Database {
        PushNotification.DepartmentJoinRequestUpdated(
            requestId = this@DepartmentMemberEntity.id.value.toKotlinUuid(),
            userSub = this@DepartmentMemberEntity.userReference.id.value,
            departmentId = this@DepartmentMemberEntity.department.id.value.toKotlinUuid(),
            isConfirmed = true,
        )
    }

    fun deniedNotification() = Database {
        PushNotification.DepartmentJoinRequestUpdated(
            requestId = this@DepartmentMemberEntity.id.value.toKotlinUuid(),
            userSub = this@DepartmentMemberEntity.userReference.id.value,
            departmentId = this@DepartmentMemberEntity.department.id.value.toKotlinUuid(),
            isConfirmed = false,
        )
    }

    fun kickedNotification() = Database {
        PushNotification.DepartmentKicked(
            requestId = this@DepartmentMemberEntity.id.value.toKotlinUuid(),
            userSub = this@DepartmentMemberEntity.userReference.id.value,
            departmentId = this@DepartmentMemberEntity.department.id.value.toKotlinUuid(),
        )
    }
}
