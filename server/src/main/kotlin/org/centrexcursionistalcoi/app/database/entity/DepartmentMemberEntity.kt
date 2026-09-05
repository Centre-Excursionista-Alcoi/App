package org.centrexcursionistalcoi.app.database.entity

import java.util.UUID
import kotlin.uuid.toKotlinUuid
import org.centrexcursionistalcoi.app.data.DepartmentMemberInfo
import org.centrexcursionistalcoi.app.data.DepartmentRole
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.table.DepartmentMembers
import org.centrexcursionistalcoi.app.push.PushNotification
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.dao.java.UUIDEntity
import org.jetbrains.exposed.v1.dao.java.UUIDEntityClass
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
    private var rolesRaw by DepartmentMembers.roles

    var roles: List<DepartmentRole>
        get() = rolesRaw.mapNotNull { DepartmentRole.fromStorageName(it) }
        set(value) { rolesRaw = value.map { it.storageName } }

    /** `true` if this member holds [role], or holds [DepartmentRole.ADMIN] (which implies every role). */
    fun hasRole(role: DepartmentRole): Boolean = role in roles || DepartmentRole.ADMIN in roles

    context(_: JdbcTransaction)
    fun toData(): DepartmentMemberInfo = DepartmentMemberInfo(
        id = id.value.toKotlinUuid(),
        userSub = userReference.id.value,
        departmentId = department.id.value.toKotlinUuid(),
        confirmed = confirmed,
        roles = roles,
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
