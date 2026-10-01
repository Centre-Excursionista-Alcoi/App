package org.centrexcursionistalcoi.app.routes

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receiveText
import io.ktor.server.resources.get
import io.ktor.server.resources.patch
import io.ktor.server.resources.post
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import org.centrexcursionistalcoi.app.CEAInfo
import org.centrexcursionistalcoi.app.data.DepartmentJoinRequest
import org.centrexcursionistalcoi.app.data.DepartmentRole
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.DepartmentEntity
import org.centrexcursionistalcoi.app.database.entity.DepartmentMemberEntity
import org.centrexcursionistalcoi.app.database.entity.FileEntity
import org.centrexcursionistalcoi.app.database.entity.UserReferenceEntity
import org.centrexcursionistalcoi.app.database.table.DepartmentMembers
import org.centrexcursionistalcoi.app.error.Error
import org.centrexcursionistalcoi.app.error.respondError
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.notifications.Push
import org.centrexcursionistalcoi.app.request.CreateDepartmentRequest
import org.centrexcursionistalcoi.app.request.UpdateDepartmentMemberRolesRequest
import org.centrexcursionistalcoi.app.request.UpdateDepartmentRequest
import org.centrexcursionistalcoi.app.security.UserSession
import org.centrexcursionistalcoi.app.security.UserSession.Companion.getUserSessionOrFail
import org.centrexcursionistalcoi.app.security.hasDepartmentRole
import org.centrexcursionistalcoi.app.serialization.list
import org.centrexcursionistalcoi.app.utils.toUuidOrNull
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq

/**
 * Fetches the department with the id given in the call parameters (`id`).
 *
 * If [requiredRole] is `null`, any logged-in user may proceed (used by e.g. `/join`, self `/leave`), and a missing
 * department responds [Error.EntityNotFound]. Otherwise the caller must be a global admin, or hold [requiredRole]
 * (or [DepartmentRole.ADMIN], which implies every role) in this department -- and, to avoid leaking whether a
 * department id exists to a caller without permissions, a missing department responds [Error.PermissionRejected]
 * instead (matching the previous behavior of this restricted path).
 *
 * If any error occurs, a response is sent to the user, and the function returns `null`.
 */
internal suspend fun RoutingContext.departmentRequest(requiredRole: DepartmentRole? = null): Pair<UserSession, DepartmentEntity>? {
    val session = getUserSessionOrFail() ?: return null

    return if (requiredRole != null) {
        val departmentId = call.parameters["id"]?.toUuidOrNull()
        val department = departmentId?.let { Database { DepartmentEntity.findById(it) } }
        if (department == null) {
            call.respondError(Error.PermissionRejected())
            return null
        }
        if (!session.isAdmin() && !session.hasDepartmentRole(department.id.value, requiredRole)) {
            call.respondError(Error.PermissionRejected())
            return null
        }
        session to department
    } else {
        val departmentId = assertIdParameter() ?: return null
        val department = Database { DepartmentEntity.findById(departmentId) }
        if (department == null) {
            call.respondError(Error.EntityNotFound(DepartmentEntity::class, departmentId))
            return null
        }
        session to department
    }
}

fun Route.departmentsRoutes() {
    provideEntityRoutes(
        resources = Api.Departments.resources,
        entityClass = DepartmentEntity,
        idTypeConverter = { it.toUuidOrNull() },
        // The default listProvider (entityClass.all()) is unrestricted for every session, including anonymous --
        // a department's own displayName/image is public (its member roster is not, see Departments.extraColumns
        // / DepartmentEntity.visibleMembersFor). Stated explicitly rather than falling through to the default
        // listProvider-scanning visibleTo, which would otherwise scan every department to confirm what's already
        // known to always be true.
        visibleTo = { _, _ -> true },
        updater = UpdateDepartmentRequest.serializer(),
        createRequestSerializer = CreateDepartmentRequest.serializer(),
        creator = { request ->
            val imageFile = request.image?.let { Database { FileEntity.newFrom(it) } }
            Database {
                DepartmentEntity.new {
                    this.displayName = request.displayName
                    this.image = imageFile
                }
            }
        },
        writePermission = EntityWritePermission(
            role = DepartmentRole.ADMIN,
            // Editing/deleting an existing department is scoped to that department's own admin role. Creating a
            // brand-new department still requires global admin "for free": nobody can hold a role in a department
            // that doesn't exist yet at creation time, so this check always falls back to admin-only for POST.
            departmentOfEntity = { it.id.value },
        ),
    )

    // Allows a user to join a department
    post<Api.Departments.Id.Join> {
        val (session, department) = departmentRequest() ?: return@post

        val member = Database {
            DepartmentMemberEntity
                .find { (DepartmentMembers.departmentId eq department.id) and (DepartmentMembers.userSub eq session.sub) }
                .firstOrNull()
        }
        if (member != null) {
            if (!member.confirmed) {
                call.response.header("CEA-Info", "pending")
                call.respondText("You have already requested to join this department. Please wait for confirmation.", status = HttpStatusCode.Conflict)
            } else {
                call.response.header("CEA-Info", "member")
                call.respondText("You are already a member of this department.", status = HttpStatusCode.Conflict)
            }
        } else {
            val confirmed = session.isAdmin() // Auto-confirm if the user is an admin

            Database {
                DepartmentMemberEntity.new {
                    this.department = department
                    this.userReference = Database { UserReferenceEntity[session.sub] }
                    this.confirmed = confirmed
                }
            }
            department.updated()

            if (confirmed) {
                call.response.header("CEA-Info", "member")
                call.respondText("You have joined the department.", status = HttpStatusCode.OK)
            } else {
                call.response.header("CEA-Info", "pending")
                call.respondText("Join request sent. Please wait for confirmation.", status = HttpStatusCode.Created)
            }
        }
    }

    post<Api.Departments.Id.Leave> {
        val (session, department) = departmentRequest() ?: return@post

        val member = Database {
            DepartmentMemberEntity
                .find { (DepartmentMembers.departmentId eq department.id) and (DepartmentMembers.userSub eq session.sub) }
                .firstOrNull()
        }
        if (member == null) {
            call.respond(HttpStatusCode.NoContent)
        } else {
            Database {
                member.delete()
            }
            department.updated()

            call.respond(HttpStatusCode.NoContent)
        }
    }
    post<Api.Departments.Id.Leave.Member> { leave ->
        val (_, department) = departmentRequest(DepartmentRole.PEOPLE_MANAGER) ?: return@post

        val sub = leave.sub

        val member = Database {
            DepartmentMemberEntity
                .find { (DepartmentMembers.departmentId eq department.id) and (DepartmentMembers.userSub eq sub) }
                .firstOrNull()
        }
        if (member == null) {
            call.respondError(Error.EntityNotFound(DepartmentMemberEntity::class, sub))
        } else {
            // Built here, not inside Push.launch: it runs without a transaction, so reading the member's references
            // there throws "Can't init value outside the transaction". And before the delete, while the row exists.
            val notification = member.kickedNotification()
            Database {
                member.delete()
            }
            department.updated()

            Push.launch {
                Push.sendPushNotification(
                    userSub = notification.userSub,
                    notification = notification,
                    includeAdmins = false,
                )
            }

            call.respond(HttpStatusCode.NoContent)
        }
    }

    get<Api.Departments.Id.Members> {
        val (session, department) = departmentRequest() ?: return@get

        // Shared with Departments.extraColumns (DepartmentEntity.visibleMembersFor) so this and GET
        // /departments/{id} can't silently diverge on who's allowed to see the roster.
        val pendingRequests = Database {
            department.visibleMembersFor(session).map { entity ->
                DepartmentJoinRequest(
                    entity.userReference.id.value,
                    entity.department.id.value,
                    entity.id.value
                )
            }
        }
        call.respondText(
            json.encodeToString(DepartmentJoinRequest.serializer().list(), pendingRequests),
            ContentType.Application.Json,
        )
    }

    // Allows an admin or people manager to confirm and deny join requests
    post<Api.Departments.Id.Confirm> { confirm ->
        val (_, department) = departmentRequest(DepartmentRole.PEOPLE_MANAGER) ?: return@post

        val requestId = confirm.requestId.toUuidOrNull()
        if (requestId == null) {
            call.respondText("Missing or malformed request id", status = HttpStatusCode.BadRequest)
            return@post
        }

        val member = Database {
            DepartmentMemberEntity
                .find { (DepartmentMembers.id eq requestId) and (DepartmentMembers.departmentId eq department.id) }
                .firstOrNull()
        }
        if (member == null) {
            call.respondText("Join request not found", status = HttpStatusCode.NotFound)
            return@post
        }

        if (member.confirmed) {
            call.response.header(HttpHeaders.CEAInfo, "member")
            call.respondText("Join request already confirmed", status = HttpStatusCode.OK)
            return@post
        }

        Database {
            member.confirmed = true
        }

        // Built here, not inside Push.launch: it runs without a transaction (see the kick route above).
        val notification = member.confirmedNotification()
        Push.launch {
            Push.sendPushNotification(
                userSub = notification.userSub,
                notification = notification,
                includeAdmins = true,
            )
        }

        call.respondText("Join request confirmed", status = HttpStatusCode.OK)
    }
    post<Api.Departments.Id.Deny> { deny ->
        val (_, department) = departmentRequest(DepartmentRole.PEOPLE_MANAGER) ?: return@post

        val requestId = deny.requestId.toUuidOrNull()
        if (requestId == null) {
            call.respondText("Missing or malformed request id", status = HttpStatusCode.BadRequest)
            return@post
        }

        val member = Database {
            DepartmentMemberEntity
                .find { (DepartmentMembers.id eq requestId) and (DepartmentMembers.departmentId eq department.id) }
                .firstOrNull()
        }
        if (member == null) {
            call.respondText("Join request not found", status = HttpStatusCode.NotFound)
            return@post
        }

        // Built here, not inside Push.launch: it runs without a transaction (see the kick route above).
        val notification = member.deniedNotification()
        Database {
            // Denied request, delete the member entry
            member.delete()
        }

        Push.launch {
            Push.sendPushNotification(
                userSub = notification.userSub,
                notification = notification,
                includeAdmins = true,
            )
        }

        call.respondText("Join request denied", status = HttpStatusCode.OK)
    }

    // Allows a department admin (or global admin) to (re)assign a confirmed member's roles within the department.
    // Gated by DepartmentRole.ADMIN specifically -- not any lesser role -- since assigning roles (including ADMIN
    // itself) is privilege-escalation-capable.
    patch<Api.Departments.Id.Members.Roles> { roles ->
        val (_, department) = departmentRequest(DepartmentRole.ADMIN) ?: return@patch

        val memberId = roles.memberId.toUuidOrNull()
        if (memberId == null) {
            call.respondError(Error.MalformedId())
            return@patch
        }

        val member = Database {
            DepartmentMemberEntity
                .find { (DepartmentMembers.id eq memberId) and (DepartmentMembers.departmentId eq department.id) }
                .firstOrNull()
        }
        if (member == null) {
            call.respondError(Error.EntityNotFound(DepartmentMemberEntity::class, memberId))
            return@patch
        }

        val body = call.receiveText()
        val request = try {
            json.decodeFromString(UpdateDepartmentMemberRolesRequest.serializer(), body)
        } catch (e: Exception) {
            call.respondError(Error.MalformedRequest())
            return@patch
        }

        Database {
            member.roles = request.roles
        }
        department.updated()

        call.respond(HttpStatusCode.NoContent)
    }
}
