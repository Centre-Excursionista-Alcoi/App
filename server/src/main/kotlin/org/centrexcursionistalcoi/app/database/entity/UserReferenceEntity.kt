package org.centrexcursionistalcoi.app.database.entity

import org.centrexcursionistalcoi.app.data.DepartmentMemberInfo
import org.centrexcursionistalcoi.app.data.LendingUser
import org.centrexcursionistalcoi.app.data.UserData
import org.centrexcursionistalcoi.app.data.UserInsurance
import org.centrexcursionistalcoi.app.database.table.UserReferences
import org.centrexcursionistalcoi.app.plugins.UserSession
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.Entity
import org.jetbrains.exposed.v1.dao.EntityClass
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction

class UserReferenceEntity(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, UserReferenceEntity>(UserReferences) {
        context(_: JdbcTransaction)
        fun getOrProvide(session: UserSession): UserReferenceEntity = findById(session.sub)?.apply {
            // Update existing user
            this.username = session.username
            this.email = session.email
            this.groups = session.groups
        } ?: new(session.sub) {
            this.username = session.username
            this.email = session.email
            this.groups = session.groups
        }
    }

    var sub by UserReferences.sub
    var username by UserReferences.username
    var email by UserReferences.email
    var groups by UserReferences.groups

    context(_: JdbcTransaction)
    fun toData(lendingUser: LendingUser?, insurances: List<UserInsurance>?, departments: List<DepartmentMemberInfo>?) = UserData(
        sub = sub.value,
        username = username,
        email = email,
        groups = groups,
        lendingUser = lendingUser,
        insurances = insurances.orEmpty(),
        departments = departments.orEmpty(),
    )
}
