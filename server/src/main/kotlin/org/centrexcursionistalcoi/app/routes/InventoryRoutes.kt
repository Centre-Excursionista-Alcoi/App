package org.centrexcursionistalcoi.app.routes

import io.ktor.server.routing.Route
import kotlinx.serialization.builtins.serializer
import org.centrexcursionistalcoi.app.data.DepartmentRole
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.DepartmentEntity
import org.centrexcursionistalcoi.app.database.entity.DepartmentMemberEntity
import org.centrexcursionistalcoi.app.database.entity.FileEntity
import org.centrexcursionistalcoi.app.database.entity.InventoryItemEntity
import org.centrexcursionistalcoi.app.database.entity.InventoryItemTypeEntity
import org.centrexcursionistalcoi.app.database.table.DepartmentMembers
import org.centrexcursionistalcoi.app.database.table.InventoryItemTypes
import org.centrexcursionistalcoi.app.database.table.InventoryItems
import org.centrexcursionistalcoi.app.database.table.LendingItems
import org.centrexcursionistalcoi.app.request.CreateInventoryItemRequest
import org.centrexcursionistalcoi.app.request.CreateInventoryItemTypeRequest
import org.centrexcursionistalcoi.app.request.UpdateInventoryItemRequest
import org.centrexcursionistalcoi.app.request.UpdateInventoryItemTypeRequest
import org.centrexcursionistalcoi.app.utils.toUuidOrNull
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.EmptySizedIterable
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

fun Route.inventoryRoutes() {
    provideEntityRoutes(
        resources = Api.Inventory.Types.resources,
        entityClass = InventoryItemTypeEntity,
        syncKey = "inventory_types",
        idTypeConverter = { it.toUuidOrNull() },
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
        visibleTo = { type, session -> type.isVisibleTo(session) },
        updater = UpdateInventoryItemTypeRequest.serializer(),
        createRequestSerializer = CreateInventoryItemTypeRequest.serializer(),
        creator = { request ->
            val deptEntity = request.department?.let { id ->
                Database { DepartmentEntity.findById(id) } ?: throw NoSuchElementException("Department with given id does not exist")
            }
            val imageFile = request.image?.let { Database { FileEntity.newFrom(it) } }

            Database {
                InventoryItemTypeEntity.new {
                    this.displayName = request.displayName
                    this.description = request.description
                    this.categories = request.categories
                    this.weight = request.weight
                    this.department = deptEntity
                    this.image = imageFile
                }
            }
        },
        writePermission = EntityWritePermission(
            role = DepartmentRole.INVENTORY_MANAGER,
            departmentOfEntity = { it.department?.id?.value },
        ),
    )
    provideEntityRoutes(
        resources = Api.Inventory.Items.resources,
        entityClass = InventoryItemEntity,
        syncKey = "inventory_items",
        idTypeConverter = { it.toUuidOrNull() },
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
        visibleTo = { item, session -> item.isVisibleTo(session) },
        deleteReferencesCheck = { item ->
            LendingItems.select(LendingItems.item)
                .where { LendingItems.item eq item.id }
                .empty()
        },
        updater = UpdateInventoryItemRequest.serializer(),
        createRequestSerializer = CreateInventoryItemRequest.serializer(),
        creator = { request ->
            val itemType = Database { InventoryItemTypeEntity.findById(request.type) }
                ?: throw NoSuchElementException("Type with given id does not exist")

            Database {
                InventoryItemEntity.new {
                    this.variation = request.variation
                    this.type = itemType
                    this.nfcId = request.nfcId
                    this.manufacturerTraceabilityCode = request.manufacturerTraceabilityCode
                }
            }
        },
        writePermission = EntityWritePermission(
            role = DepartmentRole.INVENTORY_MANAGER,
            departmentOfEntity = { it.type.department?.id?.value },
        ),
    )
}
