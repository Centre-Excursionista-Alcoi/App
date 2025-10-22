package org.centrexcursionistalcoi.app.routes

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.request.receiveMultipart
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.header
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.util.UUID
import kotlin.uuid.toKotlinUuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import org.centrexcursionistalcoi.app.ADMIN_GROUP_NAME
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.InventoryItemEntity
import org.centrexcursionistalcoi.app.database.entity.InventoryItemTypeEntity
import org.centrexcursionistalcoi.app.database.entity.LendingEntity
import org.centrexcursionistalcoi.app.database.entity.LendingUserEntity
import org.centrexcursionistalcoi.app.database.entity.UserReferenceEntity
import org.centrexcursionistalcoi.app.database.table.InventoryItems
import org.centrexcursionistalcoi.app.database.table.LendingItems
import org.centrexcursionistalcoi.app.database.table.LendingUsers
import org.centrexcursionistalcoi.app.database.table.Lendings
import org.centrexcursionistalcoi.app.database.table.UserReferences
import org.centrexcursionistalcoi.app.database.utils.encodeEntityListToString
import org.centrexcursionistalcoi.app.database.utils.encodeEntityToString
import org.centrexcursionistalcoi.app.error.Errors
import org.centrexcursionistalcoi.app.error.respondError
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.mailersend.MailerSendAttachment
import org.centrexcursionistalcoi.app.mailersend.MailerSendEmail
import org.centrexcursionistalcoi.app.notifications.Email
import org.centrexcursionistalcoi.app.notifications.Push
import org.centrexcursionistalcoi.app.plugins.UserSession.Companion.assertAdmin
import org.centrexcursionistalcoi.app.plugins.UserSession.Companion.getUserSessionOrFail
import org.centrexcursionistalcoi.app.push.PushNotification
import org.centrexcursionistalcoi.app.request.FileRequestData
import org.centrexcursionistalcoi.app.request.UpdateInventoryItemRequest
import org.centrexcursionistalcoi.app.request.UpdateInventoryItemTypeRequest
import org.centrexcursionistalcoi.app.serialization.UUIDSerializer
import org.centrexcursionistalcoi.app.serialization.list
import org.centrexcursionistalcoi.app.today
import org.centrexcursionistalcoi.app.utils.LendingUtils.conflictsWith
import org.centrexcursionistalcoi.app.utils.toUUIDOrNull
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.exceptions.UnsupportedByDialectException
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.json.contains

/**
 * Mutex to ensure that lendings are created one at a time to avoid conflicts.
 */
private val lendingsMutex = Mutex()

fun Route.inventoryRoutes() {
    provideEntityRoutes(
        base = "inventory/types",
        entityClass = InventoryItemTypeEntity,
        idTypeConverter = { UUID.fromString(it) },
        creator = { formParameters ->
            var displayName: String? = null
            var description: String? = null
            val image = FileRequestData()

            formParameters.forEachPart { partData ->
                when (partData) {
                    is PartData.FormItem -> {
                        if (partData.name == "displayName") {
                            displayName = partData.value
                        } else if (partData.name == "description") {
                            description = partData.value
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
                InventoryItemTypeEntity.new {
                    this.displayName = displayName
                    this.description = description
                    this.image = imageFile
                }
            }
        },
        updater = UpdateInventoryItemTypeRequest.serializer(),
    )
    provideEntityRoutes(
        base = "inventory/items",
        entityClass = InventoryItemEntity,
        idTypeConverter = { UUID.fromString(it) },
        creator = { formParameters ->
            var variation: String? = null
            var type: UUID? = null

            formParameters.forEachPart { partData ->
                when (partData) {
                    is PartData.FormItem -> {
                        if (partData.name == "variation") {
                            variation = partData.value
                        } else if (partData.name == "type") {
                            type = try {
                                UUID.fromString(partData.value)
                            } catch (_: IllegalArgumentException) {
                                null
                            }
                        }
                    }
                    else -> { /* nothing */ }
                }
            }

            if (type == null) {
                throw NullPointerException("Missing or invalid type")
            }

            val itemType = Database { InventoryItemTypeEntity.findById(type!!) }
            if (itemType == null) {
                throw NoSuchElementException("Type with given id does not exist")
            }

            Database {
                InventoryItemEntity.new {
                    this.variation = variation
                    this.type = itemType
                }
            }
        },
        updater = UpdateInventoryItemRequest.serializer(),
    )
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
            call.respondText("Missing or malformed 'from' date", status = HttpStatusCode.BadRequest)
            return@postWithLock
        }
        val to = try {
            toText?.let { LocalDate.parse(it) }
        } catch (_: DateTimeParseException) {
            null
        }
        if (to == null) {
            call.respondText("Missing or malformed 'to' date", status = HttpStatusCode.BadRequest)
            return@postWithLock
        }
        if (to.isBefore(from)) {
            call.respondText("'to' date cannot be before 'from' date", status = HttpStatusCode.BadRequest)
            return@postWithLock
        }

        // Make sure dates are in the future
        val today = today()
        if (from.isBefore(today) || to.isBefore(today)) {
            call.respondText("Lending dates must be in the future", status = HttpStatusCode.BadRequest)
            return@postWithLock
        }

        if (items == null) {
            call.respondText("Missing 'items' parameter", status = HttpStatusCode.BadRequest)
            return@postWithLock
        }
        val itemsIdList = items.split(',').mapNotNull { it.toUUIDOrNull() }
        if (itemsIdList.isEmpty()) {
            call.respondText("No valid item IDs in 'items' parameter", status = HttpStatusCode.BadRequest)
            return@postWithLock
        }

        val userReferenceEntity = Database { UserReferenceEntity.findById(session.sub) }
        if (userReferenceEntity == null) {
            call.respondText("User reference not found", status = HttpStatusCode.BadRequest)
            return@postWithLock
        }

        val itemsList = Database { InventoryItemEntity.find { InventoryItems.id inList itemsIdList }.toList() }
        if (itemsList.isEmpty()) {
            call.respondText("No valid item IDs in 'items' parameter", status = HttpStatusCode.BadRequest)
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
            call.respondText("User has an open lending without a submitted memory", status = HttpStatusCode.PreconditionFailed)
            return@postWithLock
        }

        // Make sure there are no conflicts with existing non-returned lendings
        val conflicts = Database { LendingEntity.find { Lendings.returned eq false }.conflictsWith(from, to, itemsList) }
        if (conflicts) {
            call.respondText("Lending conflicts with existing lending for one or more items", status = HttpStatusCode.Conflict)
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
        CoroutineScope(Dispatchers.IO).launch {
            val emails = try {
                Database {
                    UserReferenceEntity.find { UserReferences.groups.contains(ADMIN_GROUP_NAME) }
                        .map { MailerSendEmail(it.email, it.username) }
                }
            } catch (_: UnsupportedByDialectException) {
                Database {
                    UserReferenceEntity.all()
                        .filter { it.groups.contains(ADMIN_GROUP_NAME) }
                        .map { MailerSendEmail(it.email, it.username) }
                }
            }
            val (from, to) = Database { lendingEntity.from to lendingEntity.to }
            println("Sending emails to: $emails")
            Email.sendEmail(
                to = emails,
                subject = "New lending request (#${lendingEntity.id.value})",
                htmlContent = """
                    <p>A new lending request has been created by ${userReferenceEntity.username}.</p>
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
                """.trimIndent()
            )
        }
        CoroutineScope(Dispatchers.IO).launch {
            val notification = PushNotification.NewLendingRequest(lendingEntity.id.value.toKotlinUuid())
            Push.sendAdminPushNotification(notification)
        }

        call.response.header(
            HttpHeaders.Location,
            "/inventory/lendings/${lendingEntity.id.value}"
        )
        call.respondText("Lending created", status = HttpStatusCode.Created)
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
            call.respondText("Malformed lending id", status = HttpStatusCode.BadRequest)
            return@get
        }

        val lending = Database { LendingEntity.findById(lendingId) }
        if (lending == null) {
            call.respondText("Lending #$lendingId not found", status = HttpStatusCode.NotFound)
            return@get
        }

        if (!session.isAdmin()) {
            val lendingUserSub = Database { lending.userSub.sub.value }
            if (lendingUserSub != session.sub) {
                // Return not found to avoid leaking existence of the lending
                call.respondText("Lending #$lendingId not found", status = HttpStatusCode.NotFound)
                return@get
            }
        }

        call.respondText(ContentType.Application.Json) {
            json.encodeEntityToString(lending, LendingEntity)
        }
    }
    post("inventory/lendings/{id}/cancel") {
        val session = getUserSessionOrFail() ?: return@post

        val lendingId = call.parameters["id"]?.toUUIDOrNull()
        if (lendingId == null) {
            call.respondText("Malformed lending id", status = HttpStatusCode.BadRequest)
            return@post
        }

        // Find the lending by id, and make sure it belongs to the user
        // Otherwise return 404 to avoid leaking existence of the lending
        val lending = Database { LendingEntity.find { (Lendings.id eq lendingId) and (Lendings.userSub eq session.sub) }.firstOrNull() }
        if (lending == null) {
            call.respondText("Lending #$lendingId not found", status = HttpStatusCode.NotFound)
            return@post
        }

        if (lending.taken) {
            call.respondText("Lending #$lendingId has already been picked up and cannot be cancelled", status = HttpStatusCode.Conflict)
            return@post
        }

        Database { lending.delete() }

        call.respondText("Lending #$lendingId cancelled", status = HttpStatusCode.NoContent)
    }
    post("inventory/lendings/{id}/confirm") {
        assertAdmin() ?: return@post

        val lendingId = call.parameters["id"]?.toUUIDOrNull()
        if (lendingId == null) {
            call.respondText("Malformed lending id", status = HttpStatusCode.BadRequest)
            return@post
        }

        val lending = Database { LendingEntity.findById(lendingId) }
        if (lending == null) {
            call.respondText("Lending #$lendingId not found", status = HttpStatusCode.NotFound)
            return@post
        }

        Database {
            lending.confirmed = true
        }

        call.respondText("Lending #$lendingId confirmed", status = HttpStatusCode.OK)
    }
    post("inventory/lendings/{id}/pickup") {
        val session = assertAdmin() ?: return@post

        val lendingId = call.parameters["id"]?.toUUIDOrNull()
        if (lendingId == null) {
            call.respondText("Malformed lending id", status = HttpStatusCode.BadRequest)
            return@post
        }

        val lending = Database { LendingEntity.findById(lendingId) }
        if (lending == null) {
            call.respondText("Lending #$lendingId not found", status = HttpStatusCode.NotFound)
            return@post
        }

        if (!lending.confirmed) {
            call.respondText("Lending #$lendingId is not confirmed", status = HttpStatusCode.Conflict)
            return@post
        }

        val userReference = Database { UserReferenceEntity.findById(session.sub) }
        if (userReference == null) {
            call.respondText("Your user reference was not found", status = HttpStatusCode.InternalServerError)
            return@post
        }

        Database {
            lending.taken = true
            lending.givenBy = userReference.id
            lending.givenAt = Instant.now()
        }

        call.respondText("Lending #$lendingId picked up", status = HttpStatusCode.OK)
    }
    post("inventory/lendings/{id}/return") {
        val session = assertAdmin() ?: return@post

        val lendingId = call.parameters["id"]?.toUUIDOrNull()
        if (lendingId == null) {
            call.respondText("Malformed lending id", status = HttpStatusCode.BadRequest)
            return@post
        }

        val lending = Database { LendingEntity.findById(lendingId) }
        if (lending == null) {
            call.respondText("Lending #$lendingId not found", status = HttpStatusCode.NotFound)
            return@post
        }

        if (!lending.taken) {
            call.respondText("Lending #$lendingId is not taken", status = HttpStatusCode.Conflict)
            return@post
        }

        val userReference = Database { UserReferenceEntity.findById(session.sub) }
        if (userReference == null) {
            call.respondText("Your user reference was not found", status = HttpStatusCode.InternalServerError)
            return@post
        }

        Database {
            lending.returned = true
            lending.receivedBy = userReference.id
            lending.receivedAt = Instant.now()
        }

        call.respondText("Lending #$lendingId memory submitted", status = HttpStatusCode.Created)
    }
    post("inventory/lendings/{id}/add_memory") {
        assertContentType(ContentType.MultiPart.FormData)
        val session = getUserSessionOrFail() ?: return@post

        val lendingId = call.parameters["id"]?.toUUIDOrNull()
        if (lendingId == null) {
            respondError(Errors.MalformedId)
            return@post
        }

        val file = FileRequestData()
        val multiPartData = call.receiveMultipart()
        multiPartData.forEachPart { part ->
            when (part.name) {
                "file" -> {
                    if (part is PartData.FileItem) {
                        file.populate(part)
                    }
                }
                else -> { /* Ignore other parts */ }
            }
        }

        if (file.isEmpty()) {
            respondError(Errors.MissingFile)
            return@post
        }

        val lending = Database { LendingEntity.findById(lendingId) }
        if (lending == null) {
            respondError(Errors.EntityNotFound("Lending", lendingId.toString()))
            return@post
        }

        if (!lending.returned) {
            respondError(Errors.CannotSubmitMemoryUntilMaterialIsReturned)
            return@post
        }

        val userReference = Database { UserReferenceEntity.findById(session.sub) }
        if (userReference == null) {
            respondError(Errors.UserReferenceNotFound)
            return@post
        }

        // make sure the lending belongs to the user
        val lendingUserSub = Database { lending.userSub.sub.value }
        if (lendingUserSub != session.sub) {
            // Return not found to avoid leaking existence of the lending
            respondError(Errors.EntityNotFound("Lending", lendingId.toString()))
            return@post
        }

        val documentEntity = file.newEntity(false)

        Database {
            lending.memorySubmitted = true
            lending.memorySubmittedAt = Instant.now()
            lending.memoryDocument = documentEntity
        }

        // Notify administrators that a new memory has been uploaded
        CoroutineScope(Dispatchers.IO).launch {
            val admins = Database { UserReferenceEntity.find { UserReferences.groups.contains(ADMIN_GROUP_NAME) } }
            val emails = admins.map { MailerSendEmail(it.email, it.username) }
            val documentBytes = file.baos.toByteArray().also { file.close() }
            Email.sendEmail(
                to = emails,
                subject = "New lending memory submitted (#${lending.id.value})",
                htmlContent = """
                    <p>The lending memory for lending #${lending.id.value} has been submitted by ${userReference.username}.</p>
                    <p>
                        <strong>From:</strong> ${lending.from}<br/>
                        <strong>To:</strong> ${lending.to}<br/>
                        <strong>Notes:</strong> ${lending.notes ?: "None"}<br/>
                    </p>
                    <p>Please review the submitted memory in the admin panel.</p>
                """.trimIndent(),
                attachments = listOf(
                    MailerSendAttachment(documentBytes, file.originalFileName ?: "memory.pdf"),
                ),
            )
        }
        CoroutineScope(Dispatchers.IO).launch {
            val notification = PushNotification.NewLendingRequest(lendingId.toKotlinUuid())
            Push.sendAdminPushNotification(notification)
        }

        call.respondText("Lending #$lendingId returned", status = HttpStatusCode.OK)
    }
    // Checks availability and allocates items of a given type for lending. Returns a list of possible item IDs for the date range.
    // TODO: Add tests
    getWithLock("inventory/types/{id}/allocate", lendingsMutex) {
        val session = getUserSessionOrFail() ?: return@getWithLock

        val typeId = call.parameters["id"]?.toUUIDOrNull()
        if (typeId == null) {
            call.respondText("Malformed item type id", status = HttpStatusCode.BadRequest)
            return@getWithLock
        }

        val parameters = call.queryParameters
        val fromText = parameters["from"]
        val toText = parameters["to"]
        val amount = parameters["amount"]?.toIntOrNull()

        if (amount == null || amount <= 0) {
            call.respondText("Missing or invalid 'amount' parameter", status = HttpStatusCode.BadRequest)
            return@getWithLock
        }

        val from = try {
            fromText?.let { LocalDate.parse(it) }
        } catch (_: DateTimeParseException) {
            null
        }
        if (from == null) {
            call.respondText("Missing or malformed 'from' date", status = HttpStatusCode.BadRequest)
            return@getWithLock
        }
        val to = try {
            toText?.let { LocalDate.parse(it) }
        } catch (_: DateTimeParseException) {
            null
        }
        if (to == null) {
            call.respondText("Missing or malformed 'to' date", status = HttpStatusCode.BadRequest)
            return@getWithLock
        }

        if (to.isBefore(from)) {
            call.respondText("'to' date cannot be before 'from' date", status = HttpStatusCode.BadRequest)
            return@getWithLock
        }

        val today = today()
        if (to.isBefore(today)) {
            call.respondText("Lending dates must be in the future", status = HttpStatusCode.BadRequest)
            return@getWithLock
        }

        // Make sure the user is signed up for lending
        val userNotSignedUpForLending = Database { LendingUserEntity.find { LendingUsers.userSub eq session.sub }.empty() }
        if (userNotSignedUpForLending) {
            call.respondText("User not signed up for lending", status = HttpStatusCode.Forbidden)
            return@getWithLock
        }

        val type = Database { InventoryItemTypeEntity.findById(typeId) }
        if (type == null) {
            call.respondText("Item type #$typeId not found", status = HttpStatusCode.NotFound)
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
            call.respondText("Not enough available items of type #$typeId for the given date range", status = HttpStatusCode.Conflict)
            return@getWithLock
        }

        val allocatedItems = availableItems.take(amount).map { it.id.value }
        call.respondText(ContentType.Application.Json) {
            json.encodeToString(UUIDSerializer.list(), allocatedItems)
        }
    }
}
