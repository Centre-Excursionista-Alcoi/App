package org.centrexcursionistalcoi.app.routes

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receiveText
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import java.util.UUID
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant
import kotlin.uuid.toKotlinUuid
import kotlinx.serialization.KSerializer
import org.centrexcursionistalcoi.app.data.DepartmentRole
import org.centrexcursionistalcoi.app.data.DepartmentRosterMember
import org.centrexcursionistalcoi.app.data.Qualification
import org.centrexcursionistalcoi.app.data.QualificationGrant
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.DepartmentEntity
import org.centrexcursionistalcoi.app.database.entity.DepartmentMemberEntity
import org.centrexcursionistalcoi.app.database.entity.QualificationEntity
import org.centrexcursionistalcoi.app.database.table.DepartmentMembers
import org.centrexcursionistalcoi.app.database.table.Qualifications
import org.centrexcursionistalcoi.app.database.table.UserQualifications
import org.centrexcursionistalcoi.app.error.Error
import org.centrexcursionistalcoi.app.error.respondError
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.now
import org.centrexcursionistalcoi.app.plugins.UserSession
import org.centrexcursionistalcoi.app.plugins.UserSession.Companion.getUserSessionOrFail
import org.centrexcursionistalcoi.app.request.CreateQualificationRequest
import org.centrexcursionistalcoi.app.request.GrantQualificationRequest
import org.centrexcursionistalcoi.app.request.UpdateQualificationRequest
import org.centrexcursionistalcoi.app.security.hasDepartmentRole
import org.centrexcursionistalcoi.app.serialization.list
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.core.upperCase
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update

private const val NAME_MAX_LENGTH = 255
private const val ROSTER_DEFAULT_LIMIT = 50
private const val ROSTER_MAX_LIMIT = 200

private fun ResultRow.toQualificationGrant() = QualificationGrant(
    qualificationId = this[UserQualifications.qualification].value.toKotlinUuid(),
    userSub = this[UserQualifications.userSub].value,
    grantedBy = this[UserQualifications.grantedBy]?.value,
    grantedAt = this[UserQualifications.grantedAt].toKotlinInstant(),
    expiresAt = this[UserQualifications.expiresAt]?.toKotlinInstant(),
)

/** A qualification resolved from the `id` path parameter, together with the department that owns it. */
private class QualificationRequest(
    val session: UserSession,
    val qualification: QualificationEntity,
    val departmentId: UUID,
)

/**
 * Resolves the qualification in the `id` path parameter for a logged-in caller who holds at least one of
 * [anyOfRoles] in the qualification's department (or is a global admin). With no [anyOfRoles], any logged-in
 * caller may proceed.
 *
 * Qualification definitions are public to every logged-in user, so -- unlike [departmentRequest] -- a missing
 * qualification responds [Error.EntityNotFound] regardless of the caller's permissions.
 *
 * If any error occurs, a response is sent to the user, and the function returns `null`.
 */
private suspend fun RoutingContext.qualificationRequest(vararg anyOfRoles: DepartmentRole): QualificationRequest? {
    val session = getUserSessionOrFail() ?: return null
    val id = assertIdParameter() ?: return null
    val resolved = Database {
        QualificationEntity.findById(id)?.let { it to it.department.id.value }
    }
    if (resolved == null) {
        call.respondError(Error.EntityNotFound(QualificationEntity::class, id))
        return null
    }
    val (qualification, departmentId) = resolved
    if (anyOfRoles.isNotEmpty() && anyOfRoles.none { session.hasDepartmentRole(departmentId, it) }) {
        call.respondError(Error.PermissionRejected())
        return null
    }
    return QualificationRequest(session, qualification, departmentId)
}

private suspend fun <T> RoutingContext.receiveJson(serializer: KSerializer<T>): T? {
    assertContentType(ContentType.Application.Json) ?: return null
    val body = call.receiveText()
    return try {
        json.decodeFromString(serializer, body)
    } catch (_: Exception) {
        call.respondError(Error.MalformedRequest())
        null
    }
}

/** `true` if [departmentId] already has a qualification named [name] (case-insensitively), other than [exceptId]. */
private fun nameTaken(departmentId: UUID, name: String, exceptId: UUID? = null): Boolean = Database {
    QualificationEntity.find {
        var condition = (Qualifications.department eq departmentId) and (Qualifications.name.upperCase() eq name.uppercase())
        if (exceptId != null) condition = condition and (Qualifications.id neq exceptId)
        condition
    }.any()
}

fun Route.qualificationsRoutes() {
    // Qualification definitions are visible to every logged-in user: events list the ones they require.
    get("/qualifications") {
        getUserSessionOrFail() ?: return@get

        val qualifications = Database {
            QualificationEntity.all().map { it.toData() }.sortedBy { it.name.lowercase() }
        }
        call.respondText(
            json.encodeToString(Qualification.serializer().list(), qualifications),
            ContentType.Application.Json,
        )
    }

    get("/qualifications/{id}") {
        val request = qualificationRequest() ?: return@get

        val qualification = Database { request.qualification.toData() }
        call.respondText(
            json.encodeToString(Qualification.serializer(), qualification),
            ContentType.Application.Json,
        )
    }

    post("/departments/{id}/qualifications") {
        val (_, department) = departmentRequest(DepartmentRole.QUALIFICATIONS_MANAGER) ?: return@post
        val request = receiveJson(CreateQualificationRequest.serializer()) ?: return@post

        val name = request.name.trim()
        if (name.isEmpty() || name.length > NAME_MAX_LENGTH) {
            call.respondError(Error.InvalidArgument("name"))
            return@post
        }
        val departmentId = department.id.value
        if (nameTaken(departmentId, name)) {
            call.respondError(Error.QualificationAlreadyExists())
            return@post
        }

        val created = Database {
            QualificationEntity.new {
                this.department = DepartmentEntity[departmentId]
                this.name = name
                this.description = request.description?.trim()?.takeIf { it.isNotEmpty() }
            }.toData()
        }
        call.response.header(HttpHeaders.Location, "/qualifications/${created.id}")
        call.respondText(
            json.encodeToString(Qualification.serializer(), created),
            ContentType.Application.Json,
            HttpStatusCode.Created,
        )
    }

    patch("/qualifications/{id}") {
        val request = qualificationRequest(DepartmentRole.QUALIFICATIONS_MANAGER) ?: return@patch
        val qualification = request.qualification
        val update = receiveJson(UpdateQualificationRequest.serializer()) ?: return@patch
        if (update.isEmpty()) {
            call.respondError(Error.NothingToUpdate())
            return@patch
        }

        val newName = update.name?.trim()
        if (newName != null) {
            if (newName.isEmpty() || newName.length > NAME_MAX_LENGTH) {
                call.respondError(Error.InvalidArgument("name"))
                return@patch
            }
            if (nameTaken(request.departmentId, newName, exceptId = qualification.id.value)) {
                call.respondError(Error.QualificationAlreadyExists())
                return@patch
            }
        }

        val updated = Database {
            newName?.let { qualification.name = it }
            update.description?.let { qualification.description = it.trim().takeIf { d -> d.isNotEmpty() } }
            qualification.toData()
        }
        call.respondText(
            json.encodeToString(Qualification.serializer(), updated),
            ContentType.Application.Json,
        )
    }

    // Deleting a qualification also deletes every grant of it.
    delete("/qualifications/{id}") {
        val request = qualificationRequest(DepartmentRole.QUALIFICATIONS_MANAGER) ?: return@delete
        Database { request.qualification.delete() }
        call.respond(HttpStatusCode.NoContent)
    }

    // Grants are private: only the department's examiners/qualifications managers/people managers (and global
    // admins) may see who holds a qualification. Everybody else reads their own via /profile/qualifications.
    get("/qualifications/{id}/grants") {
        val request = qualificationRequest(DepartmentRole.EXAMINER, DepartmentRole.PEOPLE_MANAGER) ?: return@get

        val grants = Database {
            UserQualifications.selectAll()
                .where { UserQualifications.qualification eq request.qualification.id.value }
                .map { it.toQualificationGrant() }
        }
        call.respondText(
            json.encodeToString(QualificationGrant.serializer().list(), grants),
            ContentType.Application.Json,
        )
    }

    post("/qualifications/{id}/grants") {
        val request = qualificationRequest(DepartmentRole.EXAMINER) ?: return@post
        val body = receiveJson(GrantQualificationRequest.serializer()) ?: return@post

        val expiresAt = body.expiresAt?.toJavaInstant()
        if (expiresAt != null && !expiresAt.isAfter(now())) {
            call.respondError(Error.DateMustBeInFuture())
            return@post
        }

        // Only confirmed members of the qualification's department can be granted it. Answering "not found" for
        // anyone else (rather than a distinct error) also keeps this route from confirming whether an arbitrary
        // sub exists to a caller who can't otherwise see that user.
        val isConfirmedMember = Database {
            DepartmentMemberEntity.find {
                (DepartmentMembers.userSub eq body.userSub) and
                    (DepartmentMembers.departmentId eq request.departmentId) and
                    (DepartmentMembers.confirmed eq true)
            }.any()
        }
        if (!isConfirmedMember) {
            call.respondError(Error.EntityNotFound(DepartmentMemberEntity::class, body.userSub))
            return@post
        }

        val qualificationId = request.qualification.id.value
        val grant = Database {
            val alreadyGranted = UserQualifications.selectAll()
                .where { (UserQualifications.qualification eq qualificationId) and (UserQualifications.userSub eq body.userSub) }
                .any()
            if (alreadyGranted) {
                UserQualifications.update({ (UserQualifications.qualification eq qualificationId) and (UserQualifications.userSub eq body.userSub) }) {
                    it[grantedBy] = request.session.sub
                    it[grantedAt] = now()
                    it[UserQualifications.expiresAt] = expiresAt
                }
            } else {
                UserQualifications.insert {
                    it[qualification] = qualificationId
                    it[userSub] = body.userSub
                    it[grantedBy] = request.session.sub
                    it[grantedAt] = now()
                    it[UserQualifications.expiresAt] = expiresAt
                }
            }
            UserQualifications.selectAll()
                .where { (UserQualifications.qualification eq qualificationId) and (UserQualifications.userSub eq body.userSub) }
                .first()
                .toQualificationGrant()
        }
        call.respondText(
            json.encodeToString(QualificationGrant.serializer(), grant),
            ContentType.Application.Json,
        )
    }

    // Idempotent: revoking a grant that doesn't exist also succeeds.
    delete("/qualifications/{id}/grants/{sub}") {
        val request = qualificationRequest(DepartmentRole.EXAMINER) ?: return@delete
        val sub = call.parameters["sub"] ?: return@delete call.respondError(Error.MissingArgument("sub"))

        val qualificationId = request.qualification.id.value
        Database {
            UserQualifications.deleteWhere { (UserQualifications.qualification eq qualificationId) and (UserQualifications.userSub eq sub) }
        }
        call.respond(HttpStatusCode.NoContent)
    }

    // The caller's own qualifications, including expired ones (see QualificationGrant.expiresAt).
    get("/profile/qualifications") {
        val session = getUserSessionOrFail() ?: return@get
        val grants = Database {
            UserQualifications.selectAll()
                .where { UserQualifications.userSub eq session.sub }
                .map { it.toQualificationGrant() }
        }
        call.respondText(
            json.encodeToString(QualificationGrant.serializer().list(), grants),
            ContentType.Application.Json,
        )
    }

    // The confirmed members of a department, for an examiner to pick who to grant a qualification to. Deliberately
    // only carries what's needed to identify a member (sub, full name) -- not the email/NIF/roles that
    // GET /departments/{id}/members or /users expose to people managers.
    get("/departments/{id}/roster") {
        val (_, department) = departmentRequest(DepartmentRole.EXAMINER) ?: return@get
        val query = call.request.queryParameters["q"]?.trim()?.takeIf { it.isNotEmpty() }
        val limit = (call.request.queryParameters["limit"]?.toIntOrNull() ?: ROSTER_DEFAULT_LIMIT).coerceIn(1, ROSTER_MAX_LIMIT)

        val roster = Database {
            department.confirmedMembers
                .map { it.userReference }
                .filter { query == null || it.fullName.contains(query, ignoreCase = true) }
                .sortedBy { it.fullName.lowercase() }
                .take(limit)
                .map { DepartmentRosterMember(sub = it.sub.value, fullName = it.fullName) }
        }
        call.respondText(
            json.encodeToString(DepartmentRosterMember.serializer().list(), roster),
            ContentType.Application.Json,
        )
    }
}
