package org.centrexcursionistalcoi.app.routes

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.request.contentType
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.header
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.utils.io.copyTo
import io.ktor.utils.io.streams.asByteWriteChannel
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.util.UUID
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.InventoryItemEntity
import org.centrexcursionistalcoi.app.database.entity.InventoryItemTypeEntity
import org.centrexcursionistalcoi.app.database.entity.LendingEntity
import org.centrexcursionistalcoi.app.database.entity.UserReferenceEntity
import org.centrexcursionistalcoi.app.database.table.InventoryItems
import org.centrexcursionistalcoi.app.database.table.LendingItems
import org.centrexcursionistalcoi.app.database.utils.encodeEntityToString
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.plugins.UserSession
import org.centrexcursionistalcoi.app.plugins.UserSession.Companion.getUserSessionOrFail
import org.centrexcursionistalcoi.app.request.FileRequestData
import org.centrexcursionistalcoi.app.utils.toUUIDOrNull
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

private suspend fun RoutingContext.inventoryItemRequest(mustBeAdmin: Boolean = false): Pair<UserSession, InventoryItemEntity>? {
    val session = getUserSessionOrFail() ?: return null
    if (mustBeAdmin && !session.isAdmin()) {
        call.respondText("Admin access required", status = HttpStatusCode.Forbidden)
        return null
    }

    val itemId = call.parameters["id"]?.toUUIDOrNull()
    if (itemId == null) {
        call.respondText("Missing or malformed item id", status = HttpStatusCode.BadRequest)
        return null
    }

    val item = Database { InventoryItemEntity.findById(itemId) }
    if (item == null) {
        call.respondText("Inventory item not found", status = HttpStatusCode.NotFound)
        return null
    }

    return session to item
}

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
        }
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
        }
    )
    post("inventory/items/{id}/lendings") {
        val (session, item) = inventoryItemRequest() ?: return@post

        val contentType = call.request.contentType()
        if (!contentType.match(ContentType.Application.FormUrlEncoded)) {
            call.respondText("Content-Type must be form url-encoded. It was: $contentType", status = HttpStatusCode.BadRequest)
            return@post
        }

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
            return@post
        }
        val to = try {
            toText?.let { LocalDate.parse(it) }
        } catch (_: DateTimeParseException) {
            null
        }
        if (to == null) {
            call.respondText("Missing or malformed 'to' date", status = HttpStatusCode.BadRequest)
            return@post
        }
        if (to.isBefore(from)) {
            call.respondText("'to' date cannot be before 'from' date", status = HttpStatusCode.BadRequest)
            return@post
        }

        if (items == null) {
            call.respondText("Missing 'items' parameter", status = HttpStatusCode.BadRequest)
            return@post
        }
        val itemsIdList = items.split(',').mapNotNull { it.toUUIDOrNull() }
        if (itemsIdList.isEmpty()) {
            call.respondText("No valid item IDs in 'items' parameter", status = HttpStatusCode.BadRequest)
            return@post
        }

        val userReferenceEntity = Database { UserReferenceEntity.findById(session.sub) }
        if (userReferenceEntity == null) {
            call.respondText("User reference not found", status = HttpStatusCode.BadRequest)
            return@post
        }

        val itemsList = Database { InventoryItemEntity.find { InventoryItems.id inList itemsIdList }.toList() }
        if (itemsList.isEmpty()) {
            call.respondText("No valid item IDs in 'items' parameter", status = HttpStatusCode.BadRequest)
            return@post
        }

        // TODO: Make sure there are no conflicts with existing lendings

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
            "/inventory/items/${item.id.value}/lendings/${lendingEntity.id.value}"
        )
        call.respondText("Lending created", status = HttpStatusCode.Created)
    }
    get("inventory/items/{id}/lendings/{lending-id}") {
        val (_, item) = inventoryItemRequest() ?: return@get

        val lendingId = call.parameters["lending-id"]?.toUUIDOrNull()
        if (lendingId == null) {
            call.respondText("Malformed lending id", status = HttpStatusCode.BadRequest)
            return@get
        }

        val lending = Database { LendingEntity.findById(lendingId) }
        if (lending == null) {
            call.respondText("Lending #$lendingId not found", status = HttpStatusCode.NotFound)
            return@get
        }

        call.respondText(ContentType.Application.Json) {
            json.encodeEntityToString(lending, LendingEntity)
        }
    }
}
