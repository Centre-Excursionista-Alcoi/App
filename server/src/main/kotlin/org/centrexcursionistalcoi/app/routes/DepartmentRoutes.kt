package org.centrexcursionistalcoi.app.routes

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.response.header
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.utils.io.copyTo
import io.ktor.utils.io.streams.asByteWriteChannel
import java.io.ByteArrayOutputStream
import org.centrexcursionistalcoi.app.CEAInfo
import org.centrexcursionistalcoi.app.data.DepartmentJoinRequestsResponse
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.DepartmentEntity
import org.centrexcursionistalcoi.app.database.entity.DepartmentMemberEntity
import org.centrexcursionistalcoi.app.database.entity.FileEntity
import org.centrexcursionistalcoi.app.database.entity.UserReferenceEntity
import org.centrexcursionistalcoi.app.database.table.DepartmentMembers
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.plugins.UserSession
import org.centrexcursionistalcoi.app.plugins.UserSession.Companion.getUserSessionOrFail
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq

private suspend fun RoutingContext.departmentRequest(mustBeAdmin: Boolean = false): Pair<UserSession, DepartmentEntity>? {
    val session = getUserSessionOrFail() ?: return null
    if (mustBeAdmin && !session.isAdmin()) {
        call.respondText("Admin access required", status = HttpStatusCode.Forbidden)
        return null
    }

    val departmentId = call.parameters["id"]?.toIntOrNull()
    if (departmentId == null) {
        call.respondText("Missing or malformed department id", status = HttpStatusCode.BadRequest)
        return null
    }

    val department = Database { DepartmentEntity.findById(departmentId) }
    if (department == null) {
        call.respondText("Department not found", status = HttpStatusCode.NotFound)
        return null
    }

    return session to department
}

fun Route.departmentsRoutes() {
    provideEntityRoutes(
        base = "departments",
        entityClass = DepartmentEntity,
        idTypeConverter = { it.toIntOrNull() },
        creator = { formParameters ->
            var displayName: String? = null
            var contentType: ContentType? = null
            var originalFileName: String? = null
            val imageDataStream = ByteArrayOutputStream()

            formParameters.forEachPart { partData ->
                when (partData) {
                    is PartData.FormItem -> {
                        if (partData.name == "displayName") {
                            displayName = partData.value
                        }
                    }
                    is PartData.FileItem -> {
                        if (partData.name == "image") {
                            contentType = partData.contentType
                            originalFileName = partData.originalFileName
                            partData.provider().copyTo(imageDataStream.asByteWriteChannel())
                        }
                    }
                    else -> { /* nothing */ }
                }
            }

            if (displayName == null) {
                throw NullPointerException("Missing displayName")
            }

            Database {
                val imageFile = if (imageDataStream.size() > 0) {
                    FileEntity.new {
                        name = originalFileName ?: "unknown"
                        type = contentType?.toString() ?: "application/octet-stream"
                        data = imageDataStream.toByteArray()
                    }
                } else null

                DepartmentEntity.new {
                    this.displayName = displayName
                    this.imageFile = imageFile
                }
            }
        }
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
                    this.userSub = Database { UserReferenceEntity.getOrProvide(session).id }
                    this.confirmed = confirmed
                }
            }
            if (confirmed) {
                call.response.header("CEA-Info", "member")
                call.respondText("You have joined the department.", status = HttpStatusCode.OK)
            } else {
                call.response.header("CEA-Info", "pending")
                call.respondText("Join request sent. Please wait for confirmation.", status = HttpStatusCode.Created)
            }
        }
    }

    // Allows an admin to view pending join requests
    get("/departments/{id}/requests") {
        val (_, department) = departmentRequest(true) ?: return@get

        val pendingRequests = Database {
            DepartmentMemberEntity
                .find { (DepartmentMembers.departmentId eq department.id) and (DepartmentMembers.confirmed eq false) }
                .map { DepartmentJoinRequestsResponse.Request(it.userSub.value, it.id.value) }
        }
        call.respondText(
            json.encodeToString(DepartmentJoinRequestsResponse.serializer(), DepartmentJoinRequestsResponse(pendingRequests)),
            ContentType.Application.Json,
        )
    }

    // Allows an admin to confirm a join request
    post("/departments/{id}/confirm/{requestId}") {
        val (_, department) = departmentRequest(true) ?: return@post

        val requestId = call.parameters["requestId"]?.toIntOrNull()
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
        call.respondText("Join request confirmed", status = HttpStatusCode.OK)

    }
}
