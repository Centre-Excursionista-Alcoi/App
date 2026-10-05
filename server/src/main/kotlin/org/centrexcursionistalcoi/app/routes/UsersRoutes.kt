package org.centrexcursionistalcoi.app.routes

import io.ktor.http.HttpStatusCode
import org.centrexcursionistalcoi.app.security.UserSession
import org.centrexcursionistalcoi.app.routes.sync.SyncSections
import org.centrexcursionistalcoi.app.routes.sync.SyncSection
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.data.UserData
import org.centrexcursionistalcoi.app.data.Member
import kotlinx.serialization.builtins.ListSerializer
import io.ktor.server.resources.get
import io.ktor.server.resources.post
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import org.centrexcursionistalcoi.app.ADMIN_GROUP_NAME
import org.centrexcursionistalcoi.app.data.DepartmentRole
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.DepartmentEntity
import org.centrexcursionistalcoi.app.database.entity.DepartmentMemberEntity
import org.centrexcursionistalcoi.app.database.entity.LendingUserEntity
import org.centrexcursionistalcoi.app.database.entity.MemberEntity
import org.centrexcursionistalcoi.app.database.entity.UserInsuranceEntity
import org.centrexcursionistalcoi.app.database.entity.UserReferenceEntity
import org.centrexcursionistalcoi.app.database.table.DepartmentMembers
import org.centrexcursionistalcoi.app.database.table.LendingUsers
import org.centrexcursionistalcoi.app.database.table.UserInsurances
import org.centrexcursionistalcoi.app.database.table.UserReferences
import org.centrexcursionistalcoi.app.error.Error
import org.centrexcursionistalcoi.app.error.respondError
import org.centrexcursionistalcoi.app.security.UserSession.Companion.assertAdmin
import org.centrexcursionistalcoi.app.security.UserSession.Companion.getUserSessionOrFail
import org.centrexcursionistalcoi.app.security.isMembersManager
import org.centrexcursionistalcoi.app.security.isUsersManager
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList

/**
 * The users [session] can see: all of them for admins and users managers; for everyone else, those in the departments
 * they hold PEOPLE_MANAGER in (or only themselves, if in none).
 * @return `null` if the user of the session doesn't exist.
 */
internal fun usersFor(session: UserSession): List<UserData>? {
    var managingDepartments: List<DepartmentEntity>? = null
    if (!session.isAdmin() && !session.isUsersManager()) {
        // We have to check whether the user is a people manager of a department
        managingDepartments = Database {
            DepartmentMemberEntity.getUserDepartments(session.sub, isConfirmed = true)
                .filter { it.hasRole(DepartmentRole.PEOPLE_MANAGER) }
                .map { it.department }
        }
    }

    if (managingDepartments != null && managingDepartments.isEmpty()) {
        // Not admin and not managing any departments, return self
        val user = Database {
            UserReferenceEntity.find { UserReferences.sub eq session.sub }
                .map { it.toData() }
                .firstOrNull()
        }
        return if (user == null) null else listOf(user)
    }

    return Database {
        if (managingDepartments == null) {
            // If admin, get all users
            val departmentMembers = DepartmentMemberEntity.all().map { it.toData() }
            val lendingUsers = LendingUserEntity.all().map { it.toData() }
            val insurances = UserInsuranceEntity.all().map { it.toData() }

            UserReferenceEntity.all()
                // Avoid duplicates in case a user is in multiple departments
                .distinctBy { it.sub }
                // Map to data class including lending user and insurances
                .map { user ->
                    user.toData(
                        lendingUser = lendingUsers.find { it.sub == user.sub.value },
                        insurances = insurances.filter { it.userSub == user.sub.value },
                        departments = departmentMembers.filter { it.userSub == user.sub.value }
                    )
                }
        } else {
            // Else, get only users in the departments they manage
            // Since if reached this point the user is at least inside a department, we are sure that themself is included, so no need to check
            val userReferences = managingDepartments
                // Select members, not just confirmed members, because we need the member data in order to display requests to the user
                .flatMap { it.members }
                .map { it.userReference }
                .distinctBy { it.sub }
            val userSubs = userReferences.map { it.sub.value }

            val departmentMembers = DepartmentMemberEntity
                // Find members in the relevant departments
                .find { DepartmentMembers.userSub inList userSubs }
                .map { it.toData() }
            val lendingUsers = LendingUserEntity
                // Find members in the relevant departments
                .find { LendingUsers.userSub inList userSubs }
                .map { it.toData() }
            val insurances = UserInsuranceEntity
                // Find members in the relevant departments
                .find { UserInsurances.userSub inList userSubs }
                .map { it.toData() }

            userReferences
                // Map to data class including lending user and insurances
                .map { user ->
                    user.toData(
                        lendingUser = lendingUsers.find { it.sub == user.sub.value },
                        insurances = insurances.filter { it.userSub == user.sub.value },
                        departments = departmentMembers.filter { it.userSub == user.sub.value }
                    )
                }
        }
    }
}

/** The members [session] can see: only admins and members managers get their private data. */
internal fun membersFor(session: UserSession): List<Member> {
    val members = Database { MemberEntity.all().map { it.toMember() } }
    // Non-admins/non-Members-Managers get stripped member data
    return if (session.isAdmin() || session.isMembersManager()) members else members.map { it.strip() }
}

fun Route.usersRoutes() {
    SyncSections.register(
        SyncSection(
            key = "users",
            // If the user of the session doesn't exist the route answers an error; here there's just nothing
            snapshot = { session -> json.encodeToString(ListSerializer(UserData.serializer()), usersFor(session).orEmpty()) },
        )
    )
    SyncSections.register(
        SyncSection(
            key = "members",
            snapshot = { session -> json.encodeToString(ListSerializer(Member.serializer()), membersFor(session)) },
        )
    )

    // Provides a list of all users. Admins and Users Managers get all users, everyone else only gets users in
    // departments they hold PEOPLE_MANAGER in (if they hold that role in no department, only themselves are
    // included in the list).
    get<Api.Users> {
        val session = getUserSessionOrFail() ?: return@get
        val users = usersFor(session)
        if (users == null) {
            respondError(Error.UserNotFound())
            return@get
        }
        call.respond(users)
    }
    get<Api.Users.Sub> { user ->
        val session = getUserSessionOrFail() ?: return@get
        val sub = user.sub

        val user = Database {
            val canView = session.sub == sub || session.isAdmin() || session.isUsersManager() || run {
                val managingDepartments = DepartmentMemberEntity.getUserDepartments(session.sub, isConfirmed = true)
                    .filter { it.hasRole(DepartmentRole.PEOPLE_MANAGER) }
                    .map { it.department }
                managingDepartments.flatMap { it.members }.any { it.userReference.sub.value == sub }
            }
            if (!canView) return@Database null

            UserReferenceEntity.find { UserReferences.sub eq sub }
                .map { it.toData() }
                .firstOrNull()
        }
        if (user == null) {
            respondError(Error.EntityNotFound(UserReferenceEntity::class, sub))
            return@get
        }
        call.respond(user)
    }
    // Promote a user to admin - admin only
    post<Api.Users.Sub.Promote> { promote ->
        assertAdmin() ?: return@post

        val sub = promote.parent.sub

        // Find user reference
        val reference = Database { UserReferenceEntity.find { UserReferences.sub eq sub }.firstOrNull() }
        if (reference == null) {
            respondError(Error.UserNotFound())
            return@post
        }

        Database {
            // Add admin group to user
            val groups = reference.groups.toMutableList()
            if (!groups.contains(ADMIN_GROUP_NAME)) {
                groups.add(ADMIN_GROUP_NAME)
                reference.groups = groups
            }
        }

        call.respond(HttpStatusCode.NoContent)
    }
    get<Api.Members> {
        val session = getUserSessionOrFail() ?: return@get
        call.respond(membersFor(session))
    }
}
