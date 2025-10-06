package org.centrexcursionistalcoi.app.routes

import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.routing.Route
import io.ktor.utils.io.copyTo
import io.ktor.utils.io.streams.asByteWriteChannel
import java.util.UUID
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.InventoryItemEntity
import org.centrexcursionistalcoi.app.database.entity.InventoryItemTypeEntity
import org.centrexcursionistalcoi.app.request.FileRequestData

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
}
