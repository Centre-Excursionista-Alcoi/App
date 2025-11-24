package org.centrexcursionistalcoi.app.routes

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.request.contentLength
import io.ktor.server.request.receiveMultipart
import io.ktor.server.request.receiveParameters
import io.ktor.server.request.receiveText
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeParseException
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid
import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.SerializationException
import org.centrexcursionistalcoi.app.ADMIN_GROUP_NAME
import org.centrexcursionistalcoi.app.data.LendingMemory
import org.centrexcursionistalcoi.app.data.ReferencedInventoryItem.Companion.referenced
import org.centrexcursionistalcoi.app.data.ReferencedInventoryItemType.Companion.referenced
import org.centrexcursionistalcoi.app.data.Sports
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.DepartmentEntity
import org.centrexcursionistalcoi.app.database.entity.FileEntity
import org.centrexcursionistalcoi.app.database.entity.InventoryItemEntity
import org.centrexcursionistalcoi.app.database.entity.InventoryItemTypeEntity
import org.centrexcursionistalcoi.app.database.entity.LendingEntity
import org.centrexcursionistalcoi.app.database.entity.LendingUserEntity
import org.centrexcursionistalcoi.app.database.entity.ReceivedItemEntity
import org.centrexcursionistalcoi.app.database.entity.UserInsuranceEntity
import org.centrexcursionistalcoi.app.database.entity.UserReferenceEntity
import org.centrexcursionistalcoi.app.database.table.InventoryItems
import org.centrexcursionistalcoi.app.database.table.LendingItems
import org.centrexcursionistalcoi.app.database.table.LendingUsers
import org.centrexcursionistalcoi.app.database.table.Lendings
import org.centrexcursionistalcoi.app.database.table.UserInsurances
import org.centrexcursionistalcoi.app.database.table.UserReferences
import org.centrexcursionistalcoi.app.database.utils.encodeEntityListToString
import org.centrexcursionistalcoi.app.database.utils.encodeEntityToString
import org.centrexcursionistalcoi.app.error.Error
import org.centrexcursionistalcoi.app.error.respondError
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.mailersend.MailerSendAttachment
import org.centrexcursionistalcoi.app.mailersend.MailerSendEmail
import org.centrexcursionistalcoi.app.notifications.Email
import org.centrexcursionistalcoi.app.notifications.Push
import org.centrexcursionistalcoi.app.pdf.PdfGeneratorService
import org.centrexcursionistalcoi.app.plugins.UserSession.Companion.assertAdmin
import org.centrexcursionistalcoi.app.plugins.UserSession.Companion.getUserSessionOrFail
import org.centrexcursionistalcoi.app.request.FileRequestData
import org.centrexcursionistalcoi.app.request.ReturnLendingRequest
import org.centrexcursionistalcoi.app.serialization.UUIDSerializer
import org.centrexcursionistalcoi.app.serialization.list
import org.centrexcursionistalcoi.app.today
import org.centrexcursionistalcoi.app.utils.LendingUtils.conflictsWith
import org.centrexcursionistalcoi.app.utils.toUUIDOrNull
import org.centrexcursionistalcoi.app.utils.toUuidOrNull
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.json.contains

/**
 * Mutex to ensure that lendings are created one at a time to avoid conflicts.
 */
private val lendingsMutex = Mutex()

fun Route.lendingsRoutes() {
    postWithLock("inventory/lendings", lendingsMutex) {
        val session = getUserSessionOrFail() ?: return@postWithLock

        assertContentType(ContentType.Application.FormUrlEncoded) ?: return@postWithLock

        val parameters = call.receiveParameters()
        val fromText = parameters["from"]
        val toText = parameters["to"]
        val notes = parameters["notes"]
        val items = parameters["items"]

        val from = try {
            fromText?.let { LocalDate.parse(it) }
        } catch (_: DateTimeParseException) {
            null
        }
        if (from == null) {
            call.respondError(Error.MissingArgument("from"))
            return@postWithLock
        }
        val to = try {
            toText?.let { LocalDate.parse(it) }
        } catch (_: DateTimeParseException) {
            null
        }
        if (to == null) {
            call.respondError(Error.MissingArgument("to"))
            return@postWithLock
        }
        if (to.isBefore(from)) {
            call.respondError(Error.EndDateCannotBeBeforeStart())
            return@postWithLock
        }

        // Make sure dates are in the future
        val today = today()
        if (from.isBefore(today) || to.isBefore(today)) {
            call.respondError(Error.DateMustBeInFuture())
            return@postWithLock
        }

        if (items == null) {
            call.respondError(Error.MissingArgument("items"))
            return@postWithLock
        }
        val itemsIdList = items.split(',').mapNotNull { it.toUUIDOrNull() }
        if (itemsIdList.isEmpty()) {
            call.respondError(Error.ListCannotBeEmpty("items"))
            return@postWithLock
        }

        val userReferenceEntity = Database { UserReferenceEntity.findById(session.sub) }
        if (userReferenceEntity == null) {
            call.respondError(Error.UserReferenceNotFound())
            return@postWithLock
        }

        // Make sure the user has an active insurance
        val validInsurances = Database {
            UserInsuranceEntity
                .find { (UserInsurances.userSub eq session.sub) and (UserInsurances.validFrom lessEq from) and (UserInsurances.validTo greaterEq to) }
                .count()
        }
        if (validInsurances <= 0) {
            call.respondError(Error.UserDoesNotHaveInsurance())
            return@postWithLock
        }

        val itemsList = Database { InventoryItemEntity.find { InventoryItems.id inList itemsIdList }.toList() }
        if (itemsList.isEmpty()) {
            call.respondError(Error.ListCannotBeEmpty("items"))
            return@postWithLock
        }

        // Make sure the user has no other lending without a submitted memory
        val openLendingWithoutMemory = Database {
            LendingEntity.find {
                (Lendings.userSub eq session.sub) and
                        (Lendings.memorySubmitted eq false)
            }.empty().not()
        }
        if (openLendingWithoutMemory) {
            call.respondError(Error.MemoryNotSubmitted())
            return@postWithLock
        }

        // Make sure there are no conflicts with existing non-returned lendings
        val conflicts = Database { LendingEntity.find { Lendings.returned eq false }.conflictsWith(from, to, itemsList) }
        if (conflicts) {
            call.respondError(Error.LendingConflict())
            return@postWithLock
        }

        val lendingEntity = Database {
            transaction {
                LendingEntity.new {
                    this.userSub = userReferenceEntity
                    this.from = from
                    this.to = to
                    this.notes = notes
                }
            }.also { entity ->
                for (item in itemsList) {
                    LendingItems.insert {
                        it[LendingItems.lending] = entity.id
                        it[LendingItems.item] = item.id
                    }
                }
            }
        }

        // Notify admins asynchronously
        println("Scheduling lending notification email for lending #${lendingEntity.id.value}")
        Email.launch {
            val emails = Database {
                UserReferenceEntity.find { UserReferences.email neq null }
                    .toList()
                    .filter { it.groups.contains(ADMIN_GROUP_NAME) }
                    .mapNotNull { MailerSendEmail(it.email ?: return@mapNotNull null, it.fullName) }
            }
            val (from, to) = Database { lendingEntity.from to lendingEntity.to }
            val url = "cea://admin/lendings#${lendingEntity.id.value}"
            Email.sendEmail(
                to = emails,
                subject = "New lending request (#${lendingEntity.id.value})",
                htmlContent = """
                    <p>A new lending request has been created by ${userReferenceEntity.fullName}.</p>
                    <p>
                        <strong>From:</strong> $from<br/>
                        <strong>To:</strong> $to<br/>
                        <strong>Notes:</strong> ${lendingEntity.notes ?: "None"}<br/>
                        <strong>Items:</strong>
                        <ul>
                            ${itemsList.joinToString("\n") { "<li>${Database { it.type.displayName }} (${it.variation ?: "No variation"})</li>" }}
                        </ul>
                    </p>
                    <p>Please review and confirm the lending in the admin panel.</p>
                    <a href="$url">Open in app</a> (<a href="$url">$url</a>)
                """.trimIndent()
            )
        }
        Push.launch {
            Push.sendAdminPushNotification(lendingEntity.newNotification())
        }

        call.response.header(
            HttpHeaders.Location,
            "/inventory/lendings/${lendingEntity.id.value}"
        )
        call.respond(HttpStatusCode.Created)
    }
    getWithLock("inventory/lendings", lendingsMutex) {
        val session = getUserSessionOrFail() ?: return@getWithLock

        if (session.isAdmin()) {
            val allLendings = Database { LendingEntity.all().toList() }
            call.respondText(ContentType.Application.Json) {
                json.encodeEntityListToString(allLendings, LendingEntity)
            }
        } else {
            val userLendings = Database {
                val userRef = UserReferenceEntity.findById(session.sub)!!
                LendingEntity.find { Lendings.userSub eq userRef.id }.toList()
            }
            call.respondText(ContentType.Application.Json) {
                json.encodeEntityListToString(userLendings, LendingEntity)
            }
        }
    }
    get("inventory/lendings/{id}") {
        val session = getUserSessionOrFail() ?: return@get

        val lendingId = call.parameters["id"]?.toUUIDOrNull()
        if (lendingId == null) {
            call.respondError(Error.MalformedId())
            return@get
        }

        val lending = Database { LendingEntity.findById(lendingId) }
        if (lending == null) {
            call.respondError(Error.EntityNotFound("Lending", lendingId.toString()))
            return@get
        }

        if (!session.isAdmin()) {
            val lendingUserSub = Database { lending.userSub.sub.value }
            if (lendingUserSub != session.sub) {
                // Return not found to avoid leaking existence of the lending
                call.respondError(Error.EntityNotFound("Lending", lendingId.toString()))
                return@get
            }
        }

        call.respondText(ContentType.Application.Json) {
            json.encodeEntityToString(lending, LendingEntity)
        }
    }
    delete("inventory/lendings/{id}") {
        assertAdmin() ?: return@delete

        val lendingId = call.parameters["id"]?.toUUIDOrNull()
        if (lendingId == null) {
            call.respondError(Error.MalformedId())
            return@delete
        }

        val lending = Database { LendingEntity.findById(lendingId) }
        if (lending == null) {
            call.respondError(Error.EntityNotFound("Lending", lendingId.toString()))
            return@delete
        }

        Database { lending.delete() }

        call.respond(HttpStatusCode.NoContent)
    }
    post("inventory/lendings/{id}/cancel") {
        val session = getUserSessionOrFail() ?: return@post

        val lendingId = call.parameters["id"]?.toUUIDOrNull()
        if (lendingId == null) {
            call.respondError(Error.MalformedId())
            return@post
        }

        // Find the lending by id, and make sure it belongs to the user
        // Otherwise return 404 to avoid leaking existence of the lending
        val lending = Database { LendingEntity.find { (Lendings.id eq lendingId) and (Lendings.userSub eq session.sub) }.firstOrNull() }
        if (lending == null) {
            call.respondError(Error.EntityNotFound("Lending", lendingId.toString()))
            return@post
        }

        if (lending.taken) {
            call.respondError(Error.LendingAlreadyPickedUp())
            return@post
        }

        Database { lending.delete() }

        // Send push notification to the owner of the lending asynchronously
        Push.launch {
            Push.sendPushNotification(
                reference = Database { lending.userSub },
                notification = lending.cancelledNotification()
            )
        }

        call.respond(HttpStatusCode.NoContent)
    }
    post("inventory/lendings/{id}/confirm") {
        assertAdmin() ?: return@post

        val lendingId = call.parameters["id"]?.toUUIDOrNull()
        if (lendingId == null) {
            call.respondError(Error.MalformedId())
            return@post
        }

        val lending = Database { LendingEntity.findById(lendingId) }
        if (lending == null) {
            call.respondError(Error.EntityNotFound("Lending", lendingId.toString()))
            return@post
        }

        Database {
            lending.confirmed = true
        }

        // Send Push Notification asynchronously
        Push.launch {
            Push.sendPushNotification(
                reference = Database { lending.userSub },
                notification = lending.confirmedNotification()
            )
        }

        call.respond(HttpStatusCode.NoContent)
    }
    post("inventory/lendings/{id}/pickup") {
        val session = assertAdmin() ?: return@post

        val lendingId = call.parameters["id"]?.toUUIDOrNull()
        if (lendingId == null) {
            call.respondError(Error.MalformedId())
            return@post
        }

        val lending = Database { LendingEntity.findById(lendingId) }
        if (lending == null) {
            call.respondError(Error.EntityNotFound("Lending", lendingId.toString()))
            return@post
        }

        if (!lending.confirmed) {
            call.respondError(Error.LendingNotConfirmed())
            return@post
        }

        val userReference = Database { UserReferenceEntity.findById(session.sub) }
        if (userReference == null) {
            call.respondError(Error.UserReferenceNotFound())
            return@post
        }

        val contentLength = call.request.contentLength()
        if (contentLength != null && contentLength > 0) {
            assertContentType(ContentType.Application.FormUrlEncoded) ?: return@post
            val parameters = call.receiveParameters()
            val dismissItemsParam = parameters["dismiss_items"]
            val dismissItems = dismissItemsParam?.split(',')?.mapNotNull { it.toUUIDOrNull() } ?: emptyList()
            Database {
                for (itemId in dismissItems) {
                    LendingItems.deleteWhere { (LendingItems.lending eq lending.id) and (LendingItems.item eq itemId) }
                }
            }
            call.response.header("CEA-Dismissed-Items", dismissItems.joinToString(","))
        }

        Database {
            lending.taken = true
            lending.givenBy = userReference.id
            lending.givenAt = Instant.now()
        }

        // Send Push Notification asynchronously
        Push.launch {
            Push.sendAdminPushNotification(lending.takenNotification(false))
            Push.sendPushNotification(
                reference = Database { lending.userSub },
                notification = lending.takenNotification(true)
            )
        }

        call.respond(HttpStatusCode.NoContent)
    }
    post("inventory/lendings/{id}/return") {
        assertContentType(ContentType.Application.Json) ?: return@post
        val session = assertAdmin() ?: return@post

        val lendingId = call.parameters["id"]?.toUUIDOrNull()
        if (lendingId == null) {
            call.respondError(Error.MalformedId())
            return@post
        }

        val lending = Database { LendingEntity.findById(lendingId) }
        if (lending == null) {
            call.respondError(Error.EntityNotFound("Lending", lendingId.toString()))
            return@post
        }

        if (!lending.taken) {
            call.respondError(Error.LendingNotTaken(lendingId.toKotlinUuid()))
            return@post
        }

        val userReference = Database { UserReferenceEntity.findById(session.sub) }
        if (userReference == null) {
            call.respondError(Error.UserReferenceNotFound())
            return@post
        }

        val body = call.receiveText()
        val request = try {
            json.decodeFromString(ReturnLendingRequest.serializer(), body)
        } catch (e: SerializationException) {
            call.respondError(Error.SerializationError(e.message, body))
            return@post
        }

        if (request.returnedItems.isEmpty()) {
            call.respondError(Error.ListCannotBeEmpty("returned_items"))
            return@post
        }

        // Make sure all the returned item ids are valid
        val returnedItems = Database {
            request.returnedItems.map { item -> InventoryItemEntity.findById(item.itemId.toJavaUuid()) }
        }
        if (returnedItems.any { it == null }) {
            call.respondError(Error.InvalidItemInReturnedItems())
            return@post
        }
        @Suppress("UNCHECKED_CAST")
        returnedItems as List<InventoryItemEntity>

        for (item in returnedItems) {
            Database {
                ReceivedItemEntity.new {
                    this.lending = lending
                    this.item = item
                    this.notes = request.returnedItems.find { it.itemId == item.id.value.toKotlinUuid() }?.notes
                    this.receivedBy = userReference
                    this.receivedAt = Instant.now()
                }
            }
        }

        // Check that all items in the lending have been returned
        val missingItemsIds = Database {
            LendingEntity[lendingId].items.filter { itemEntity -> returnedItems.find { it.id == itemEntity.id } == null }
                .map { it.id.value.toKotlinUuid() }
        }

        if (missingItemsIds.isEmpty()) {
            Database {
                lending.returned = true
            }

            // Send Push Notification asynchronously
            Push.launch {
                Push.sendAdminPushNotification(
                    notification = lending.returnedNotification(false)
                )
                Push.sendPushNotification(
                    reference = Database { lending.userSub },
                    notification = lending.returnedNotification(true)
                )
            }

            call.respond(HttpStatusCode.NoContent)
        } else {
            Push.launch {
                Push.sendAdminPushNotification(lending.partialReturnNotification(false))
            }

            call.response.header("CEA-Missing-Items", missingItemsIds.joinToString(","))
            call.respond(HttpStatusCode.Accepted)
        }
    }
    post("inventory/lendings/{id}/add_memory") {
        assertContentType(ContentType.MultiPart.FormData)
        val session = getUserSessionOrFail() ?: return@post

        val lendingId = call.parameters["id"]?.toUUIDOrNull()
        if (lendingId == null) {
            respondError(Error.MalformedId())
            return@post
        }

        var place: String? = null
        var users: List<String>? = null
        var externalUsers: String? = null
        var plainText: String? = null
        var sport: Sports? = null
        var departmentId: Uuid? = null
        var attachedFiles: List<FileRequestData> = emptyList()

        val multiPartData = call.receiveMultipart()
        multiPartData.forEachPart { part ->
            if (part is PartData.FormItem) {
                val name = part.name
                when {
                    name == "place" -> {
                        place = part.value.takeIf { it.isNotBlank() }
                    }
                    name == "users" -> {
                        val usersList = part.value.split(',')
                        users = usersList.ifEmpty { null }
                    }
                    name == "external_users" -> {
                        externalUsers = part.value.takeIf { it.isNotBlank() }
                    }
                    name == "text" -> {
                        plainText = part.value.takeIf { it.isNotBlank() }
                    }
                    name == "department" -> {
                        departmentId = part.value.toUuidOrNull()
                    }
                    name == "sport" -> {
                        sport = try {
                            Sports.valueOf(part.value)
                        } catch (_: IllegalArgumentException) {
                            null
                        }
                    }
                    name?.startsWith("file_") == true -> {
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

        val lending = Database { LendingEntity.findById(lendingId) }
        if (lending == null) {
            respondError(Error.EntityNotFound("Lending", lendingId.toString()))
            return@post
        }

        if (!lending.returned) {
            respondError(Error.CannotSubmitMemoryUntilMaterialIsReturned())
            return@post
        }

        val userReference = Database { UserReferenceEntity.findById(session.sub) }
        if (userReference == null) {
            respondError(Error.UserReferenceNotFound())
            return@post
        }

        // make sure the lending belongs to the user
        val lendingUserSub = Database { lending.userSub.sub.value }
        if (lendingUserSub != session.sub) {
            // Return not found to avoid leaking existence of the lending
            respondError(Error.EntityNotFound("Lending", lendingId.toString()))
            return@post
        }

        // If given, make sure the department exists
        val department = departmentId?.let { id ->
            val department = Database { DepartmentEntity.findById(id.toJavaUuid()) }
            if (department == null) {
                respondError(Error.EntityNotFound(DepartmentEntity::class, id.toString()))
                return@post
            }
            department
        }

        // Store all attachments
        val documentEntities = attachedFiles.map { file ->
            Database { file.newEntity() }
        }

        // Instantiate the memory
        val memory = LendingMemory(
            place = place,
            memberUsers = users.orEmpty(),
            externalUsers = externalUsers,
            text = plainText!!,
            sport = sport,
            department = departmentId,
            files = documentEntities.map { it.id.value.toKotlinUuid() }
        )

        // Generate the PDF file for the memory
        val baos = ByteArrayOutputStream()
        baos.use { output ->
            val departments = Database { DepartmentEntity.all().map { it.toData() } }
            PdfGeneratorService.generateLendingPdf(
                memory.referenced(
                    users = Database {
                        UserReferenceEntity.find { UserReferences.sub inList memory.memberUsers }.map { it.toData(null, null, null) }
                    },
                    departments = departments,
                ),
                itemsUsed = Database {
                    lending.items.map { item ->
                        item.toData().referenced(item.type.toData().referenced(departments))
                    }
                },
                submittedBy = userReference.fullName,
                dateRange = lending.from to lending.to,
                photoProvider = { uuid -> Database { FileEntity[uuid].bytes } },
                outputStream = output,
            )
        }
        val pdfDocumentEntity = Database {
            FileEntity.new {
                name = "lending_memory_${lending.id.value}.pdf"
                contentType = ContentType.Application.Pdf
                bytes = baos.toByteArray()
            }
        }

        Database {
            lending.memorySubmitted = true
            lending.memorySubmittedAt = Instant.now()
            lending.memory = memory
            lending.memoryPdf = pdfDocumentEntity
        }

        // Notify administrators that a new memory has been uploaded
        Email.launch {
            val emails = Database {
                UserReferenceEntity.find { UserReferences.email neq null and UserReferences.groups.contains(ADMIN_GROUP_NAME) }
                    .mapNotNull { MailerSendEmail(it.email ?: return@mapNotNull null, it.fullName) }
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
            Push.sendAdminPushNotification(
                notification = lending.memoryAddedNotification()
            )
            Push.sendPushNotification(
                reference = Database { lending.userSub },
                notification = lending.memoryAddedNotification()
            )
        }

        call.respond(HttpStatusCode.NoContent)
    }
    // Allows admins to skip the memory submission for a lending
    post("inventory/lendings/{id}/skip_memory") {
        assertAdmin() ?: return@post

        val lendingId = call.parameters["id"]?.toUUIDOrNull()
        if (lendingId == null) {
            respondError(Error.MalformedId())
            return@post
        }

        val lending = Database { LendingEntity.findById(lendingId) }
        if (lending == null) {
            respondError(Error.EntityNotFound("Lending", lendingId.toString()))
            return@post
        }

        Database {
            lending.memorySubmitted = true
            lending.memorySubmittedAt = Instant.now()
        }

        Push.launch {
            Push.sendAdminPushNotification(lending.memoryAddedNotification())
            Push.sendPushNotification(
                reference = Database { lending.userSub },
                notification = lending.memoryAddedNotification()
            )
        }

        call.respond(HttpStatusCode.NoContent)
    }
    // Checks availability and allocates items of a given type for lending. Returns a list of possible item IDs for the date range.
    getWithLock("inventory/types/{id}/allocate", lendingsMutex) {
        val session = getUserSessionOrFail() ?: return@getWithLock

        val typeId = call.parameters["id"]?.toUUIDOrNull()
        if (typeId == null) {
            call.respondError(Error.MalformedId())
            return@getWithLock
        }

        val parameters = call.queryParameters
        val fromText = parameters["from"]
        val toText = parameters["to"]
        val amount = parameters["amount"]?.toIntOrNull()

        if (amount == null || amount <= 0) {
            call.respondError(Error.MissingArgument("amount"))
            return@getWithLock
        }

        val from = try {
            fromText?.let { LocalDate.parse(it) }
        } catch (_: DateTimeParseException) {
            null
        }
        if (from == null) {
            call.respondError(Error.MissingArgument("from"))
            return@getWithLock
        }
        val to = try {
            toText?.let { LocalDate.parse(it) }
        } catch (_: DateTimeParseException) {
            null
        }
        if (to == null) {
            call.respondError(Error.MissingArgument("to"))
            return@getWithLock
        }

        if (to.isBefore(from)) {
            call.respondError(Error.InvalidArgument(message = "'to' date cannot be before 'from' date"))
            return@getWithLock
        }

        val today = today()
        if (to.isBefore(today)) {
            call.respondError(Error.InvalidArgument(message = "dates must be in the future"))
            return@getWithLock
        }

        // Make sure the user is signed up for lending
        val userNotSignedUpForLending = Database { LendingUserEntity.find { LendingUsers.userSub eq session.sub }.empty() }
        if (userNotSignedUpForLending) {
            call.respondError(Error.UserNotSignedUpForLending())
            return@getWithLock
        }

        // Make sure the user has an active insurance
        val validInsurances = Database {
            UserInsuranceEntity
                .find { (UserInsurances.userSub eq session.sub) and (UserInsurances.validFrom lessEq from) and (UserInsurances.validTo greaterEq to) }
                .count()
        }
        if (validInsurances <= 0) {
            call.respondError(Error.UserDoesNotHaveInsurance())
            return@getWithLock
        }

        val type = Database { InventoryItemTypeEntity.findById(typeId) }
        if (type == null) {
            call.respondError(Error.EntityNotFound("Item Type", typeId.toString()))
            return@getWithLock
        }

        val lendingEntitiesForRange = Database { LendingEntity.find { (Lendings.from greaterEq from) and (Lendings.to lessEq to) } }
        val availableItems = Database {
            InventoryItemEntity.find { InventoryItems.type eq typeId }.filter { item ->
                !lendingEntitiesForRange.conflictsWith(from, to, listOf(item))
            }.toList()
        }
        if (availableItems.size < amount) {
            call.response.header("CEA-Available-Items", availableItems.joinToString(",") { it.id.value.toString() })
            call.respondError(Error.LendingConflict())
            return@getWithLock
        }

        val allocatedItems = availableItems.take(amount).map { it.id.value }
        call.respondText(ContentType.Application.Json) {
            json.encodeToString(UUIDSerializer.list(), allocatedItems)
        }
    }
}
