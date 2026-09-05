package org.centrexcursionistalcoi.app.routes

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.request.receiveText
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import kotlin.uuid.toKotlinUuid
import org.centrexcursionistalcoi.app.CEAInfo
import org.centrexcursionistalcoi.app.data.DepartmentJoinRequest
import org.centrexcursionistalcoi.app.data.DepartmentRole
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.DepartmentEntity
import org.centrexcursionistalcoi.app.database.entity.DepartmentMemberEntity
import org.centrexcursionistalcoi.app.database.entity.UserReferenceEntity
import org.centrexcursionistalcoi.app.database.table.DepartmentMembers
import org.centrexcursionistalcoi.app.error.Error
import org.centrexcursionistalcoi.app.error.respondError
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.notifications.Push
import org.centrexcursionistalcoi.app.plugins.UserSession
import org.centrexcursionistalcoi.app.plugins.UserSession.Companion.getUserSessionOrFail
import org.centrexcursionistalcoi.app.request.FileRequestData
import org.centrexcursionistalcoi.app.request.UpdateDepartmentMemberRolesRequest
import org.centrexcursionistalcoi.app.request.UpdateDepartmentRequest
import org.centrexcursionistalcoi.app.security.hasDepartmentRole
import org.centrexcursionistalcoi.app.serialization.list
import org.centrexcursionistalcoi.app.utils.toUUIDOrNull
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
private suspend fun RoutingContext.departmentRequest(requiredRole: DepartmentRole? = null): Pair<UserSession, DepartmentEntity>? {
    val session = getUserSessionOrFail() ?: return null

    return if (requiredRole != null) {
        val departmentId = call.parameters["id"]?.toUUIDOrNull()
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
        base = "departments",
        entityClass = DepartmentEntity,
        idTypeConverter = { it.toUUIDOrNull() },
        creator = { formParameters ->
            var displayName: String? = null
            val image = FileRequestData()

            formParameters.forEachPart { partData ->
                when (partData) {
                    is PartData.FormItem -> {
                        if (partData.name == "displayName") {
                            displayName = partData.value
                        } else if (partData.name == "image") {
                            image.populate(partData)
                        }
                    }
                    is PartData.FileItem -> {
                        if (partData.name == "image") {
                            image.populate(partData)
                        }
                    }
                    else -> { /* nothing */ }
                }
            }

            if (displayName == null) {
                throw NullPointerException("Missing displayName")
            }

            val imageFile = if (image.isNotEmpty()) {
                image.newEntity()
            } else null
            Database {
                DepartmentEntity.new {
                    this.displayName = displayName
                    this.image = imageFile
                }
            }
        },
        updater = UpdateDepartmentRequest.serializer(),
        writePermission = EntityWritePermission(
            role = DepartmentRole.ADMIN,
            // Editing/deleting an existing department is scoped to that department's own admin role. Creating a
            // brand-new department still requires global admin "for free": nobody can hold a role in a department
            // that doesn't exist yet at creation time, so this check always falls back to admin-only for POST.
            departmentOfEntity = { it.id.value },
        ),
    )

    // Allows a user to join a department
    post("/departments/{id}/join") {
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

    post("/departments/{id}/leave") {
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
    post("/departments/{id}/leave/{sub}") {
        val (_, department) = departmentRequest(DepartmentRole.PEOPLE_MANAGER) ?: return@post

        val sub = call.parameters["sub"]
        if (sub == null) {
            // in theory this should never happen due to the route structure
            call.respondError(Error.MissingArgument("sub"))
            return@post
        }

        val member = Database {
            DepartmentMemberEntity
                .find { (DepartmentMembers.departmentId eq department.id) and (DepartmentMembers.userSub eq sub) }
                .firstOrNull()
        }
        if (member == null) {
            call.respondError(Error.EntityNotFound(DepartmentMemberEntity::class, sub))
        } else {
            Database {
                member.delete()
            }
            department.updated()

            Push.launch {
                Push.sendPushNotification(
                    userSub = member.userReference.id.value,
                    notification = member.kickedNotification(),
                    includeAdmins = false,
                )
            }

            call.respond(HttpStatusCode.NoContent)
        }
    }

    get("/departments/{id}/members") {
        val (session, department) = departmentRequest() ?: return@get

        val pendingRequests = Database {
            if (session.isAdmin() || session.hasDepartmentRole(department.id.value, DepartmentRole.PEOPLE_MANAGER)) {
                DepartmentMemberEntity.find { (DepartmentMembers.departmentId eq department.id) }
            } else {
                // There should only be one match or none
                DepartmentMemberEntity.find { (DepartmentMembers.departmentId eq department.id) and (DepartmentMembers.userSub eq session.sub) }
            }
                .map { entity ->
                    DepartmentJoinRequest(
                        entity.userReference.id.value,
                        entity.department.id.value.toKotlinUuid(),
                        entity.id.value.toKotlinUuid()
                    )
                }
        }
        call.respondText(
            json.encodeToString(DepartmentJoinRequest.serializer().list(), pendingRequests),
            ContentType.Application.Json,
        )
    }

    // Allows an admin or people manager to confirm and deny join requests
    post("/departments/{id}/confirm/{requestId}") {
        val (_, department) = departmentRequest(DepartmentRole.PEOPLE_MANAGER) ?: return@post

        val requestId = call.parameters["requestId"]?.toUUIDOrNull()
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

        Push.launch {
            Push.sendPushNotification(
                userSub = member.userReference.id.value,
                notification = member.confirmedNotification(),
                includeAdmins = true,
            )
        }

        call.respondText("Join request confirmed", status = HttpStatusCode.OK)
    }
    post("/departments/{id}/deny/{requestId}") {
        val (_, department) = departmentRequest(DepartmentRole.PEOPLE_MANAGER) ?: return@post

        val requestId = call.parameters["requestId"]?.toUUIDOrNull()
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

        Database {
            // Denied request, delete the member entry
            member.delete()
        }

        Push.launch {
            Push.sendPushNotification(
                userSub = member.userReference.id.value,
                notification = member.deniedNotification(),
                includeAdmins = true,
            )
        }

        call.respondText("Join request denied", status = HttpStatusCode.OK)
    }

    // Allows a department admin (or global admin) to (re)assign a confirmed member's roles within the department.
    // Gated by DepartmentRole.ADMIN specifically -- not any lesser role -- since assigning roles (including ADMIN
    // itself) is privilege-escalation-capable.
    patch("/departments/{id}/members/{memberId}/roles") {
        val (_, department) = departmentRequest(DepartmentRole.ADMIN) ?: return@patch

        val memberId = call.parameters["memberId"]?.toUUIDOrNull()
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
