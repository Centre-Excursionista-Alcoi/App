package org.centrexcursionistalcoi.app.routes

import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.routing.Route
import io.sentry.Sentry
import java.util.UUID
import kotlin.io.encoding.Base64
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.serializer
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.DepartmentEntity
import org.centrexcursionistalcoi.app.database.entity.DepartmentMemberEntity
import org.centrexcursionistalcoi.app.database.entity.InventoryItemEntity
import org.centrexcursionistalcoi.app.database.entity.InventoryItemTypeEntity
import org.centrexcursionistalcoi.app.database.table.DepartmentMembers
import org.centrexcursionistalcoi.app.database.table.InventoryItemTypes
import org.centrexcursionistalcoi.app.database.table.InventoryItems
import org.centrexcursionistalcoi.app.database.table.LendingItems
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.request.FileRequestData
import org.centrexcursionistalcoi.app.request.UpdateInventoryItemRequest
import org.centrexcursionistalcoi.app.request.UpdateInventoryItemTypeRequest
import org.centrexcursionistalcoi.app.serialization.list
import org.centrexcursionistalcoi.app.utils.toUUIDOrNull
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.EmptySizedIterable
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

fun Route.inventoryRoutes() {
    provideEntityRoutes(
        base = "inventory/types",
        entityClass = InventoryItemTypeEntity,
        idTypeConverter = { UUID.fromString(it) },
        listProvider = { session ->
            if (session == null) EmptySizedIterable()
            else if (session.isAdmin()) InventoryItemTypeEntity.all()
            else {
                val userDepartments = transaction {
                    DepartmentMemberEntity.find { (DepartmentMembers.userSub eq session.sub) and (DepartmentMembers.confirmed eq true) }
                        .map { it.department.id.value }
                }
                InventoryItemTypeEntity.find {
                    (InventoryItemTypes.department eq null) or (InventoryItemTypes.department inList userDepartments)
                }
            }
        },
        creator = { formParameters ->
            var displayName: String? = null
            var description: String? = null
            var categories: List<String>? = null
            var department: UUID? = null
            val image = FileRequestData()

            formParameters.forEachPart { partData ->
                when (partData) {
                    is PartData.FormItem -> {
                        when (partData.name) {
                            "displayName" -> displayName = partData.value
                            "description" -> description = partData.value
                            "categories" -> categories = partData.value.let {
                                try {
                                    json.decodeFromString(String.serializer().list(), it)
                                } catch (e: SerializationException) {
                                    Sentry.captureException(e)
                                    null
                                } catch (e: IllegalArgumentException) {
                                    Sentry.captureException(e)
                                    null
                                }
                            }
                            "department" -> department = partData.value.toUUIDOrNull()
                            "image" -> {
                                image.populate(partData)
                            }
                        }
                    }
                    is PartData.FileItem -> {
                        when (partData.name) {
                            "image" -> {
                                image.populate(partData)
                            }
                        }
                    }
                    else -> { /* nothing */ }
                }
            }

            if (displayName == null) {
                throw NullPointerException("Missing displayName")
            }

            val deptEntity = department?.let { id ->
                Database { DepartmentEntity.findById(id) } ?: throw NoSuchElementException("Department with given id does not exist")
            }

            val imageFile = if (image.isNotEmpty()) {
                image.newEntity()
            } else null
            Database {
                InventoryItemTypeEntity.new {
                    this.displayName = displayName
                    this.description = description
                    this.categories = categories
                    this.department = deptEntity
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
        listProvider = { session ->
            if (session == null) EmptySizedIterable()
            else if (session.isAdmin()) InventoryItemEntity.all()
            else {
                val userDepartments = transaction {
                    DepartmentMemberEntity.find { (DepartmentMembers.userSub eq session.sub) and (DepartmentMembers.confirmed eq true) }
                        .map { it.department.id.value }
                }
                val itemTypesForDepartments = transaction {
                    InventoryItemTypeEntity.find {
                        (InventoryItemTypes.department eq null) or (InventoryItemTypes.department inList userDepartments)
                    }.map { it.id.value }
                }
                InventoryItemEntity.find {
                    InventoryItems.type inList itemTypesForDepartments
                }
            }
        },
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
