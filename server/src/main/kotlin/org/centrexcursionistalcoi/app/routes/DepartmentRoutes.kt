package org.centrexcursionistalcoi.app.routes

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.response.header
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.utils.io.copyTo
import io.ktor.utils.io.streams.asByteWriteChannel
import java.io.ByteArrayOutputStream
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.DepartmentEntity
import org.centrexcursionistalcoi.app.database.entity.DepartmentMemberEntity
import org.centrexcursionistalcoi.app.database.entity.FileEntity
import org.centrexcursionistalcoi.app.database.table.DepartmentMembers
import org.centrexcursionistalcoi.app.plugins.UserSession.Companion.getUserSessionOrFail
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq

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
                    this.imageFile = imageFile?.id
                }
            }
        }
    )

    // Allows a user to join a department
    post("/departments/{id}/join") {
        val session = getUserSessionOrFail() ?: return@post

        val departmentId = call.parameters["id"]?.toIntOrNull()
        if (departmentId == null) {
            call.respondText("Missing or malformed department id", status = HttpStatusCode.BadRequest)
            return@post
        }

        val department = Database { DepartmentEntity.findById(departmentId) }
        if (department == null) {
            call.respondText("Department not found", status = HttpStatusCode.NotFound)
            return@post
        }

        val member = Database {
            DepartmentMemberEntity
                .find { (DepartmentMembers.departmentId eq departmentId) and (DepartmentMembers.userSub eq session.sub) }
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
            Database {
                DepartmentMemberEntity.new {
                    this.department = department
                    this.userSub = session.sub
                }
            }
            call.response.header("CEA-Info", "pending")
            call.respondText("Join request sent. Please wait for confirmation.", status = HttpStatusCode.Created)
        }
    }
}
