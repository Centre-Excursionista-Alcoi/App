package org.centrexcursionistalcoi.app.security

import io.ktor.server.routing.RoutingContext
import java.util.UUID
import org.centrexcursionistalcoi.app.MEMBERS_MANAGER_GROUP_NAME
import org.centrexcursionistalcoi.app.USERS_MANAGER_GROUP_NAME
import org.centrexcursionistalcoi.app.data.DepartmentRole
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.DepartmentMemberEntity
import org.centrexcursionistalcoi.app.database.table.DepartmentMembers
import org.centrexcursionistalcoi.app.error.Error
import org.centrexcursionistalcoi.app.error.respondError
import org.centrexcursionistalcoi.app.plugins.UserSession
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq

/**
 * `true` if this session is a global admin, or holds a confirmed [DepartmentMembers] row for [departmentId] whose
 * roles contain [role] or [DepartmentRole.ADMIN] (department-admin implies every other department role).
 */
fun UserSession.hasDepartmentRole(departmentId: UUID, role: DepartmentRole): Boolean {
    if (isAdmin()) return true
    return Database {
        DepartmentMemberEntity
            .find { (DepartmentMembers.userSub eq sub) and (DepartmentMembers.departmentId eq departmentId) and (DepartmentMembers.confirmed eq true) }
            .firstOrNull()
            ?.hasRole(role)
            ?: false
    }
}

/** `true` if this session holds [role] (or [DepartmentRole.ADMIN]) in *any* confirmed department membership, or is a global admin. */
fun UserSession.hasAnyDepartmentRole(role: DepartmentRole): Boolean {
    if (isAdmin()) return true
    return Database {
        DepartmentMemberEntity.getUserDepartments(sub, isConfirmed = true).any { it.hasRole(role) }
    }
}

/** General (non-department) role: manages user accounts globally. Implied by [UserSession.isAdmin]. */
fun UserSession.isUsersManager(): Boolean = isAdmin() || USERS_MANAGER_GROUP_NAME in groups

/** General (non-department) role: manages the federation member roster. Implied by [UserSession.isAdmin]. */
fun UserSession.isMembersManager(): Boolean = isAdmin() || MEMBERS_MANAGER_GROUP_NAME in groups

/**
 * Asserts that [session] holds [role] (or [DepartmentRole.ADMIN]) in the department identified by [departmentId],
 * or is a global admin. Mirrors [UserSession.Companion.assertAdmin]'s early-return style: responds
 * [Error.PermissionRejected] and returns `null` on failure.
 */
suspend fun RoutingContext.assertDepartmentRole(session: UserSession, departmentId: UUID, role: DepartmentRole): UserSession? {
    if (!session.hasDepartmentRole(departmentId, role)) {
        respondError(Error.PermissionRejected())
        return null
    }
    return session
}
