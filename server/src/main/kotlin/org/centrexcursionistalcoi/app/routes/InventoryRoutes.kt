package org.centrexcursionistalcoi.app.routes

import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.routing.Route
import java.util.UUID
import kotlin.io.encoding.Base64
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.InventoryItemEntity
import org.centrexcursionistalcoi.app.database.entity.InventoryItemTypeEntity
import org.centrexcursionistalcoi.app.database.table.LendingItems
import org.centrexcursionistalcoi.app.request.FileRequestData
import org.centrexcursionistalcoi.app.request.UpdateInventoryItemRequest
import org.centrexcursionistalcoi.app.request.UpdateInventoryItemTypeRequest
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.select

fun Route.inventoryRoutes() {
    provideEntityRoutes(
        base = "inventory/types",
        entityClass = InventoryItemTypeEntity,
        idTypeConverter = { UUID.fromString(it) },
        creator = { formParameters ->
            var displayName: String? = null
            var description: String? = null
            var category: String? = null
            val image = FileRequestData()

            formParameters.forEachPart { partData ->
                when (partData) {
                    is PartData.FormItem -> {
                        when (partData.name) {
                            "displayName" -> displayName = partData.value
                            "description" -> description = partData.value
                            "category" -> category = partData.value
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
                    this.category = category
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
            var nfcId: ByteArray? = null

            formParameters.forEachPart { partData ->
                when (partData) {
                    is PartData.FormItem -> {
                        when (partData.name) {
                            "variation" -> variation = partData.value
                            "type" -> type = try {
                                UUID.fromString(partData.value)
                            } catch (_: IllegalArgumentException) {
                                null
                            }
                            "nfcId" -> {
                                val bytes = Base64.UrlSafe.decode(partData.value)
                                println("Decoded nfcId! Bytes: ${bytes.joinToString(",") { it.toString() }}")
                                nfcId = bytes
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
                    this.nfcId = nfcId
                }
            }
        },
        deleteReferencesCheck = { item ->
            LendingItems.select(LendingItems.item)
                .where { LendingItems.item eq item.id }
                .empty()
        },
        updater = UpdateInventoryItemRequest.serializer(),
    )
}
