package org.centrexcursionistalcoi.app.routes

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.request.receiveMultipart
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
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toKotlinLocalDate
import kotlinx.serialization.SerializationException
import org.centrexcursionistalcoi.app.ADMIN_GROUP_NAME
import org.centrexcursionistalcoi.app.data.ReferencedInventoryItem.Companion.referenced
import org.centrexcursionistalcoi.app.data.ReferencedInventoryItemType.Companion.referenced
import org.centrexcursionistalcoi.app.data.Sports
import org.centrexcursionistalcoi.app.data.ZonedDateTime
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.DepartmentEntity
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
import org.centrexcursionistalcoi.app.plugins.UserSession
import org.centrexcursionistalcoi.app.plugins.UserSession.Companion.assertAdmin
import org.centrexcursionistalcoi.app.plugins.UserSession.Companion.getUserSessionOrFail
import org.centrexcursionistalcoi.app.request.FileRequestData
import org.centrexcursionistalcoi.app.request.UpdateMemoryRequest
import org.centrexcursionistalcoi.app.utils.toUUIDOrNull
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.SizedCollection
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.io.ByteArrayOutputStream
import java.util.UUID

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
    val id = call.parameters["id"]?.toUUIDOrNull()
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
    val isAllowed = isOwnerOrAdmin || (!requireOwnerOrAdmin && Database {
        val userMemberNumber = UserReferenceEntity.findById(session.sub)?.memberNumber
        userMemberNumber != null && memory.members.any { it.memberNumber == userMemberNumber }
    })
    if (!isAllowed) {
        respondError(Error.PermissionRejected())
        return null
    }

    return memory
}

/**
 * (Re)generates the memory's summary PDF from its current data and stores it as [MemoryEntity.pdf], deleting the
 * previous one (if any). Must be called after all field changes (including patched ones) have already been persisted.
 */
private fun regenerateMemoryPdf(memory: MemoryEntity) {
    val baos = ByteArrayOutputStream()
    baos.use { output ->
        val (referencedMemory, itemsUsed, submittedByName) = Database {
            val users = UserReferenceEntity.all().map { it.toData() }
            val departments = DepartmentEntity.all().map { it.toData() }
            val referencedMemory = memory.toData().referenced(
                users = users,
                members = memory.members.map { it.toMember() },
                departments = departments,
            )
            val itemsUsed = memory.lending?.items?.toList().orEmpty().map { item ->
                item.toData().referenced(item.type.toData().referenced(departments))
            }
            Triple(referencedMemory, itemsUsed, memory.submittedBy.fullName)
        }
        PdfGeneratorService.generateLendingPdf(
            referencedMemory,
            itemsUsed = itemsUsed,
            submittedBy = submittedByName,
            photoProvider = { uuid -> Database { FileEntity[uuid].bytes } },
            outputStream = output,
        )
    }

    Database {
        val oldPdf = memory.pdf
        memory.pdf = FileEntity.new {
            name = "memory_${memory.id.value}.pdf"
            contentType = ContentType.Application.Pdf
            bytes = baos.toByteArray()
        }
        oldPdf?.delete()
    }
}

fun Route.memoriesRoutes() {
    post("memories") {
        assertContentType(ContentType.MultiPart.FormData) ?: return@post
        val session = getUserSessionOrFail() ?: return@post

        var place: String? = null
        var members: List<UInt>? = null
        var externalUsers: String? = null
        var plainText: String? = null
        var sport: Sports? = null
        var departmentId: UUID? = null
        var lendingId: UUID? = null
        var fromRaw: ZonedDateTime? = null
        var toRaw: ZonedDateTime? = null
        var attachedFiles: List<FileRequestData> = emptyList()

        val multiPartData = call.receiveMultipart()
        multiPartData.forEachPart { part ->
            if (part is PartData.FormItem) {
                when (part.name) {
                    "place" -> place = part.value.takeIf { it.isNotBlank() }
                    "members" -> {
                        val membersList = part.value.split(',').mapNotNull { it.toUIntOrNull() }
                        members = membersList.ifEmpty { null }
                    }
                    "external_users" -> externalUsers = part.value.takeIf { it.isNotBlank() }
                    "text" -> plainText = part.value.takeIf { it.isNotBlank() }
                    "department" -> departmentId = part.value.toUUIDOrNull()
                    "lending" -> lendingId = part.value.toUUIDOrNull()
                    "from" -> fromRaw = runCatching { ZonedDateTime.parse(part.value) }.getOrNull()
                    "to" -> toRaw = runCatching { ZonedDateTime.parse(part.value) }.getOrNull()
                    "sport" -> {
                        sport = try {
                            Sports.valueOf(part.value)
                        } catch (_: IllegalArgumentException) {
                            null
                        }
                    }
                    else -> if (part.name?.startsWith("file_") == true) {
                        val data = FileRequestData()
                        data.populate(part)
                        attachedFiles = attachedFiles + data
                    }
                }
            } else if (part is PartData.FileItem) {
                if (part.name?.startsWith("file_") == true) {
                    val data = FileRequestData()
                    data.populate(part)
                    attachedFiles = attachedFiles + data
                }
            }
        }

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
                ZonedDateTime(zone, lending.from.toKotlinLocalDate(), LocalTime(0, 0, 0)) to
                    ZonedDateTime(zone, lending.to.toKotlinLocalDate(), LocalTime(23, 59, 59))
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
            if (toRaw!!.toInstant() < fromRaw!!.toInstant()) {
                respondError(Error.EndDateCannotBeBeforeStart())
                return@post
            }
            fromRaw!! to toRaw!!
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

        // Store all attachments
        val documentEntities = attachedFiles.map { file -> Database { file.newEntity() } }

        val memoryId = UUID.randomUUID()
        val memoryEntity = Database {
            MemoryEntity.new(memoryId) {
                this.place = place
                this.externalPeople = externalUsers
                this.text = plainText!!
                this.sport = sport
                this.department = department
                this.submittedBy = userReference
                this.from = from
                this.to = to
                this.lending = lending
            }.also { entity ->
                entity.members = SizedCollection(MemberEntity.find { Members.id inList members.orEmpty() }.toList())
                for (fileEntity in documentEntities) {
                    MemoriesFiles.insert {
                        it[memory] = entity.id
                        it[file] = fileEntity.id
                    }
                }
            }
        }

        // Generate the summary PDF for the memory
        regenerateMemoryPdf(memoryEntity)

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
                for ((i, file) in attachedFiles.withIndex()) {
                    val fileBytes = file.baos.toByteArray()
                    bytesCounter += fileBytes.size
                    if (bytesCounter > maxTotalSizeBytes) {
                        break
                    }
                    fileAttachments.add(MailerSendAttachment(fileBytes, file.originalFileName ?: "memory_attachment_$i"))
                }

                val url = "cea://admin/lendings#${lending.id.value}"
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
    get("memories") {
        val session = getUserSessionOrFail() ?: return@get

        val memories = Database {
            if (session.isAdmin()) {
                MemoryEntity.all().toList()
            } else {
                // Regular users see memories they submitted, plus memories they're tagged as a participant on.
                val userMemberNumber = UserReferenceEntity.findById(session.sub)?.memberNumber
                val taggedMemoryIds = userMemberNumber?.let { memberNumber ->
                    MemoriesMembers.selectAll().where { MemoriesMembers.member eq memberNumber }.map { it[MemoriesMembers.memory] }
                }.orEmpty()
                MemoryEntity.find { (Memories.submittedBy eq session.sub) or (Memories.id inList taggedMemoryIds) }.toList()
            }
        }

        call.respondText(ContentType.Application.Json) {
            json.encodeEntityListToString(memories, MemoryEntity)
        }
    }
    get("memories/{id}") {
        val session = getUserSessionOrFail() ?: return@get
        val memory = memoryRequest(session, requireOwnerOrAdmin = false) ?: return@get

        call.respondText(ContentType.Application.Json) {
            json.encodeEntityToString(memory, MemoryEntity)
        }
    }
    patch("memories/{id}") {
        val session = getUserSessionOrFail() ?: return@patch
        assertContentType(ContentType.Application.Json) ?: return@patch
        val memory = memoryRequest(session) ?: return@patch

        val body = call.receiveText()
        val request = try {
            json.decodeFromString(UpdateMemoryRequest.serializer(), body)
        } catch (e: SerializationException) {
            respondError(Error.SerializationError(e.message, body))
            return@patch
        }
        if (request.isEmpty()) {
            respondError(Error.NothingToUpdate())
            return@patch
        }

        Database { memory.patch(request) }
        regenerateMemoryPdf(memory)
        memory.updated()

        call.respond(HttpStatusCode.NoContent)
    }
    delete("memories/{id}") {
        assertAdmin() ?: return@delete

        val id = call.parameters["id"]?.toUUIDOrNull()
        if (id == null) {
            respondError(Error.MalformedId())
            return@delete
        }
        val memory = Database { MemoryEntity.findById(id) }
        if (memory == null) {
            respondError(Error.EntityNotFound("Memory", id.toString()))
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
