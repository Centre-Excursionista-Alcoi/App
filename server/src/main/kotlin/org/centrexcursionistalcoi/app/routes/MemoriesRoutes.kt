package org.centrexcursionistalcoi.app.routes

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.resources.delete
import io.ktor.server.resources.get
import io.ktor.server.resources.patch
import io.ktor.server.resources.post
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import org.centrexcursionistalcoi.app.ADMIN_GROUP_NAME
import org.centrexcursionistalcoi.app.AppLinks
import org.centrexcursionistalcoi.app.data.DepartmentRole
import org.centrexcursionistalcoi.app.data.ZonedDateTime
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.DepartmentEntity
import org.centrexcursionistalcoi.app.database.entity.DepartmentMemberEntity
import org.centrexcursionistalcoi.app.database.entity.FileEntity
import org.centrexcursionistalcoi.app.database.entity.LendingEntity
import org.centrexcursionistalcoi.app.database.entity.MemberEntity
import org.centrexcursionistalcoi.app.database.entity.MemoryEntity
import org.centrexcursionistalcoi.app.database.entity.UserReferenceEntity
import org.centrexcursionistalcoi.app.database.table.Lendings
import org.centrexcursionistalcoi.app.database.table.Members
import org.centrexcursionistalcoi.app.database.table.Memories
import org.centrexcursionistalcoi.app.database.table.MemoriesFiles
import org.centrexcursionistalcoi.app.database.table.MemoriesMembers
import org.centrexcursionistalcoi.app.database.utils.encodeEntityListToString
import org.centrexcursionistalcoi.app.database.utils.encodeEntityToString
import org.centrexcursionistalcoi.app.error.Error
import org.centrexcursionistalcoi.app.error.respondError
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.notifications.Email
import org.centrexcursionistalcoi.app.notifications.Push
import org.centrexcursionistalcoi.app.notifications.email.mailersend.MailerSendAttachment
import org.centrexcursionistalcoi.app.notifications.email.mailersend.MailerSendEmail
import org.centrexcursionistalcoi.app.now
import org.centrexcursionistalcoi.app.pdf.PdfGeneratorService
import org.centrexcursionistalcoi.app.request.CreateMemoryRequest
import org.centrexcursionistalcoi.app.request.MissingPartException
import org.centrexcursionistalcoi.app.request.UpdateMemoryRequest
import org.centrexcursionistalcoi.app.request.assertRequestWithFilesContentType
import org.centrexcursionistalcoi.app.request.receiveRequestWithFiles
import org.centrexcursionistalcoi.app.routes.sync.SyncSection
import org.centrexcursionistalcoi.app.routes.sync.SyncSections
import org.centrexcursionistalcoi.app.security.FileReadWriteRules
import org.centrexcursionistalcoi.app.security.UserSession
import org.centrexcursionistalcoi.app.security.UserSession.Companion.getUserSessionOrFail
import org.centrexcursionistalcoi.app.security.hasDepartmentRole
import org.centrexcursionistalcoi.app.storage.FileStorageProvider
import org.centrexcursionistalcoi.app.utils.toUuidOrNull
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.SizedCollection
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.slf4j.LoggerFactory
import kotlin.uuid.Uuid

/**
 * Fetches the memory with the id given in the call parameters (`id`), making sure the requesting session is allowed
 * to see it: admins can see every memory, and regular users can always see memories they submitted themselves.
 *
 * If [requireOwnerOrAdmin] is `false`, a regular user tagged as a participating member of the memory is also allowed
 * through -- used for read access. Modifying a memory ([requireOwnerOrAdmin] `true`, the default) always stays
 * restricted to the submitter or an admin, regardless of tagging.
 *
 * If any error occurs, a response is sent to the user, and the function returns `null`.
 */
private suspend fun RoutingContext.memoryRequest(session: UserSession, requireOwnerOrAdmin: Boolean = true): MemoryEntity? {
    val id = call.parameters["id"]?.toUuidOrNull()
    if (id == null) {
        respondError(Error.MalformedId())
        return null
    }

    val memory = Database { MemoryEntity.findById(id) }
    if (memory == null) {
        respondError(Error.EntityNotFound("Memory", id.toString()))
        return null
    }

    val isOwnerOrAdmin = session.isAdmin() || Database { memory.submittedBy.sub.value } == session.sub
    val isDeptManager = Database { memory.department }?.let { department ->
        session.hasDepartmentRole(department.id.value, DepartmentRole.MEMORY_MANAGER)
    } == true
    val isAllowed = isOwnerOrAdmin || isDeptManager || (!requireOwnerOrAdmin && Database {
        val userMemberNumber = UserReferenceEntity.findById(session.sub)?.memberNumber
        userMemberNumber != null && memory.members.any { it.memberNumber == userMemberNumber }
    })
    if (!isAllowed) {
        respondError(Error.PermissionRejected())
        return null
    }

    return memory
}

private class StoredAttachment(val objectKey: String, val size: Long, val name: String)

private val logger = LoggerFactory.getLogger("MemoriesRoutes")

/**
 * The memories [session] can see: all for admins; for everyone else those they submitted, those they're tagged as a
 * participant on, and those of the departments they're a MEMORY_MANAGER of.
 */
internal fun memoriesFor(session: UserSession): List<MemoryEntity> {
    return Database {
        if (session.isAdmin()) {
            MemoryEntity.all().toList()
        } else {
            // Regular users see memories they submitted, memories they're tagged as a participant on, plus
            // memories of departments they're a MEMORY_MANAGER of (mirroring how lendings extend list
            // visibility to managed departments).
            val userMemberNumber = UserReferenceEntity.findById(session.sub)?.memberNumber
            val taggedMemoryIds = userMemberNumber?.let { memberNumber ->
                MemoriesMembers.selectAll().where { MemoriesMembers.member eq memberNumber }.map { it[MemoriesMembers.memory] }
            }.orEmpty()
            val managedDepartmentIds = DepartmentMemberEntity.getUserDepartments(session.sub, isConfirmed = true)
                .filter { it.hasRole(DepartmentRole.MEMORY_MANAGER) }
                .map { it.department.id.value }
            MemoryEntity.find {
                (Memories.submittedBy eq session.sub) or
                    (Memories.id inList taggedMemoryIds) or
                    (Memories.department inList managedDepartmentIds)
            }.toList()
        }
    }
}

fun Route.memoriesRoutes() {
    SyncSections.register(
        SyncSection(
            key = "memories",
            snapshot = { session -> json.encodeEntityListToString(memoriesFor(session), MemoryEntity) },
        )
    )
    post<Api.Memories> {
        val session = getUserSessionOrFail() ?: return@post

        val received = receiveRequestWithFiles(CreateMemoryRequest.serializer()) ?: return@post
        val request = received.request
        val place = request.place?.takeIf { it.isNotBlank() }
        val externalUsers = request.externalUsers?.takeIf { it.isNotBlank() }
        val plainText = request.text.takeIf { it.isNotBlank() }
        val departmentId = request.department
        val lendingId = request.lending
        val fromRaw = request.from
        val toRaw = request.to

        if (plainText == null) {
            respondError(Error.MemoryNotGiven())
            return@post
        }

        val userReference = Database { UserReferenceEntity.findById(session.sub) }
        if (userReference == null) {
            respondError(Error.UserReferenceNotFound())
            return@post
        }

        // If given, make sure the lending exists, belongs to the user, and doesn't already have a memory
        val lending = lendingId?.let { id ->
            val lendingEntity = Database { LendingEntity.findById(id) }
            if (lendingEntity == null) {
                respondError(Error.EntityNotFound("Lending", id.toString()))
                return@post
            }

            // make sure the lending belongs to the user
            val lendingUserSub = Database { lendingEntity.userSub.sub.value }
            if (lendingUserSub != session.sub) {
                // Return not found to avoid leaking existence of the lending
                respondError(Error.EntityNotFound("Lending", id.toString()))
                return@post
            }

            if (!lendingEntity.returned) {
                respondError(Error.CannotSubmitMemoryUntilMaterialIsReturned())
                return@post
            }

            if (Database { lendingEntity.memory } != null) {
                respondError(Error.MemoryAlreadySubmitted())
                return@post
            }

            lendingEntity
        }

        // For lending memories, the date range is taken from the lending itself. Standalone memories must provide it.
        val (from, to) = if (lending != null) {
            val zone = TimeZone.currentSystemDefault()
            Database {
                ZonedDateTime(zone, lending.from, LocalTime(0, 0, 0)) to
                    ZonedDateTime(zone, lending.to, LocalTime(23, 59, 59))
            }
        } else {
            if (fromRaw == null) {
                respondError(Error.MissingArgument("from"))
                return@post
            }
            if (toRaw == null) {
                respondError(Error.MissingArgument("to"))
                return@post
            }
            if (toRaw.toInstant() < fromRaw.toInstant()) {
                respondError(Error.EndDateCannotBeBeforeStart())
                return@post
            }
            fromRaw to toRaw
        }

        // If given, make sure the department exists
        val department = departmentId?.let { deptId ->
            val departmentEntity = Database { DepartmentEntity.findById(deptId) }
            if (departmentEntity == null) {
                respondError(Error.EntityNotFound(DepartmentEntity::class, deptId.toString()))
                return@post
            }
            departmentEntity
        }

        // Best-effort restriction: see the comment on the memory PDF's rules below -- department MEMORY_MANAGERs and
        // tagged members can see the memory's data but not download these files.
        val attachmentRules = FileReadWriteRules(readUsers = listOf(session.sub), readGroups = listOf(ADMIN_GROUP_NAME))
        val (memoryEntity, attachments) = try {
            received.withUploads {
                Database {
                    val files = request.attachments.filterNot { it.isEmpty() }.map { file ->
                        FileEntity.newFrom(file, attachmentRules)
                    }
                    val entity = MemoryEntity.new(Uuid.random()) {
                        this.place = place
                        this.externalPeople = externalUsers
                        this.text = plainText
                        this.sport = request.sport
                        this.department = department
                        this.submittedBy = userReference
                        this.from = from
                        this.to = to
                        this.lending = lending
                    }
                    entity.members = SizedCollection(MemberEntity.find { Members.id inList request.members }.toList())
                    for (fileEntity in files) {
                        MemoriesFiles.insert {
                            it[memory] = entity.id
                            it[file] = fileEntity.id
                        }
                    }
                    // Read now: the files' values can't be read outside a transaction later
                    entity to files.mapIndexed { i, file ->
                        StoredAttachment(file.objectKey, file.size, file.name ?: "memory_attachment_$i")
                    }
                }
            }
        } catch (e: MissingPartException) {
            logger.error("Memory request refers to a missing part", e)
            respondError(Error.MalformedRequest())
            return@post
        }

        // Generate the summary PDF for the memory
        PdfGeneratorService.generateMemoryPdf(memoryEntity)

        memoryEntity.updated()

        if (lending != null) {
            Database {
                lending.memorySubmitted = true
                lending.memorySubmittedAt = now()
            }

            // Notify administrators that a new memory has been uploaded
            Email.launch {
                val emails = Database {
                    UserReferenceEntity.all()
                        .toList()
                        .filter { it.groups.contains(ADMIN_GROUP_NAME) }
                        .map { MailerSendEmail(it.email, it.fullName) }
                }

                val fileAttachments = mutableListOf<MailerSendAttachment>()
                var bytesCounter = 0L
                val maxTotalSizeBytes = 20 * 1024 * 1024 // 20 MB
                for (attachment in attachments) {
                    // Checked before reading them from the storage
                    bytesCounter += attachment.size
                    if (bytesCounter > maxTotalSizeBytes) {
                        break
                    }
                    val fileBytes = FileStorageProvider.current.readBytes(attachment.objectKey)
                    fileAttachments.add(MailerSendAttachment(fileBytes, attachment.name))
                }

                val url = AppLinks.adminLending(lending.id.value)
                Email.sendEmail(
                    to = emails,
                    subject = "New lending memory submitted (#${lending.id.value})",
                    htmlContent = """
                        <p>The lending memory for lending #${lending.id.value} has been submitted by ${userReference.fullName}.</p>
                        <p>
                            <strong>From:</strong> ${lending.from}<br/>
                            <strong>To:</strong> ${lending.to}<br/>
                            <strong>Notes:</strong> ${lending.notes ?: "None"}<br/>
                        </p>
                        <p>Please review the submitted memory in the admin panel.</p>
                        <a href="$url">Open in app</a> (<a href="$url">$url</a>)
                    """.trimIndent(),
                    attachments = fileAttachments,
                )
            }
            Push.launch {
                Push.sendPushNotification(
                    reference = Database { lending.userSub },
                    notification = lending.memoryAddedNotification(),
                    includeAdmins = true,
                )
            }
        }

        call.response.header(HttpHeaders.Location, "/memories/${memoryEntity.id.value}")
        call.respond(HttpStatusCode.Created)
    }
    get<Api.Memories> {
        val session = getUserSessionOrFail() ?: return@get
        val memories = memoriesFor(session)

        call.respondText(ContentType.Application.Json) {
            json.encodeEntityListToString(memories, MemoryEntity)
        }
    }
    get<Api.Memories.Id> {
        val session = getUserSessionOrFail() ?: return@get
        val memory = memoryRequest(session, requireOwnerOrAdmin = false) ?: return@get

        call.respondText(ContentType.Application.Json) {
            json.encodeEntityToString(memory, MemoryEntity)
        }
    }
    patch<Api.Memories.Id> {
        val session = getUserSessionOrFail() ?: return@patch
        assertRequestWithFilesContentType() ?: return@patch
        val memory = memoryRequest(session) ?: return@patch

        // As JSON, or as multipart with the attachments in parts of their own (see RequestWithFiles)
        val received = receiveRequestWithFiles(UpdateMemoryRequest.serializer()) { e, body ->
            Error.SerializationError(e.message, body)
        } ?: return@patch
        val request = received.request
        if (request.isEmpty()) {
            respondError(Error.NothingToUpdate())
            return@patch
        }

        // The patch can reassign `department` -- a MEMORY_MANAGER of the memory's current department must also
        // hold that role in the requested destination, or they could move a memory into a department they don't
        // manage. Applying the patch and re-checking inside the same transaction keeps the move atomic: on
        // rejection, the exception propagates out of the `Database { }` block and rolls the reassignment back.
        try {
            received.withUploads {
                Database {
                    memory.patch(request)
                    val newDepartment = memory.department
                    val isAllowed = session.isAdmin() ||
                        Database { memory.submittedBy.sub.value } == session.sub ||
                        (newDepartment != null && session.hasDepartmentRole(newDepartment.id.value, DepartmentRole.MEMORY_MANAGER))
                    if (!isAllowed) throw PermissionDeniedException()
                }
            }
        } catch (e: MissingPartException) {
            respondError(Error.SerializationError(e.message, null))
            return@patch
        } catch (_: PermissionDeniedException) {
            respondError(Error.PermissionRejected())
            return@patch
        }
        PdfGeneratorService.generateMemoryPdf(memory)
        memory.updated()

        call.respond(HttpStatusCode.NoContent)
    }
    delete<Api.Memories.Id> { memory ->
        val session = getUserSessionOrFail() ?: return@delete

        val id = memory.id.toUuidOrNull()
        if (id == null) {
            respondError(Error.MalformedId())
            return@delete
        }
        val memory = Database { MemoryEntity.findById(id) }
        if (memory == null) {
            respondError(Error.EntityNotFound("Memory", id.toString()))
            return@delete
        }

        val isDeptManager = Database { memory.department }?.let { department ->
            session.hasDepartmentRole(department.id.value, DepartmentRole.MEMORY_MANAGER)
        } == true
        if (!session.isAdmin() && !isDeptManager) {
            respondError(Error.PermissionRejected())
            return@delete
        }

        // Deleting a lending's memory resets the lending back to "memory not submitted", which would let its owner
        // create a new lending again. If a new lending has already been created since this memory was submitted,
        // that new lending's existence already depended on this memory being present, so the deletion must be
        // rejected to avoid retroactively invalidating it.
        val newerLendingExists = Database {
            memory.lending?.let { lending ->
                LendingEntity.find {
                    (Lendings.id neq lending.id) and
                        (Lendings.userSub eq lending.userSub.id) and
                        (Lendings.timestamp greater memory.createdAt)
                }.empty().not()
            } ?: false
        }
        if (newerLendingExists) {
            respondError(Error.CannotDeleteMemoryLendingCreatedAfter())
            return@delete
        }

        Database {
            memory.lending?.let { lending ->
                lending.memorySubmitted = false
                lending.memorySubmittedAt = null
                lending.memoryReviewed = false
            }
            memory.delete()
        }

        call.respondText("memory deleted", status = HttpStatusCode.NoContent)
    }
}
