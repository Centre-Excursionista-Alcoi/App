package org.centrexcursionistalcoi.app.routes

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.header
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.utils.io.copyTo
import io.ktor.utils.io.streams.asByteWriteChannel
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.util.UUID
import kotlinx.coroutines.sync.Mutex
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
import org.centrexcursionistalcoi.app.database.utils.encodeEntityListToString
import org.centrexcursionistalcoi.app.database.utils.encodeEntityToString
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.plugins.UserSession.Companion.assertAdmin
import org.centrexcursionistalcoi.app.plugins.UserSession.Companion.getUserSessionOrFail
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
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

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
                            image.contentType = partData.contentType
                            image.originalFileName = partData.originalFileName
                            partData.provider().copyTo(image.dataStream.asByteWriteChannel())
                        }
                    }
                    else -> { /* nothing */ }
                }
            }

            if (displayName == null) {
                throw NullPointerException("Missing displayName")
            }

            Database {
                val imageFile = if (image.isNotEmpty()) {
                    image.newEntity()
                } else null

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

        // Make sure there are no conflicts with existing lendings
        val conflicts = Database { LendingEntity.all().conflictsWith(from, to, itemsList) }
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
            call.respondText("Not enough available items of type #$typeId for the given date range", status = HttpStatusCode.Conflict)
            return@getWithLock
        }

        val allocatedItems = availableItems.take(amount).map { it.id.value }
        call.respondText(ContentType.Application.Json) {
            json.encodeToString(UUIDSerializer.list(), allocatedItems)
        }
    }
}
