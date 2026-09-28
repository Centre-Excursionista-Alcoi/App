package org.centrexcursionistalcoi.app.database.entity

import org.centrexcursionistalcoi.app.data.InventoryItemType
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.base.EntityPatcher
import org.centrexcursionistalcoi.app.database.entity.base.ImageContainerEntity
import org.centrexcursionistalcoi.app.database.entity.base.LastUpdateEntity
import org.centrexcursionistalcoi.app.database.table.InventoryItemTypes
import org.centrexcursionistalcoi.app.now
import org.centrexcursionistalcoi.app.request.UpdateInventoryItemTypeRequest
import org.centrexcursionistalcoi.app.routes.helper.notifyUpdateForEntity
import org.centrexcursionistalcoi.app.security.UserSession
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UuidEntity
import org.jetbrains.exposed.v1.dao.UuidEntityClass
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import kotlin.uuid.Uuid

class InventoryItemTypeEntity(id: EntityID<Uuid>): UuidEntity(id), LastUpdateEntity, EntityDataConverter<InventoryItemType, Uuid>, EntityPatcher<UpdateInventoryItemTypeRequest>, ImageContainerEntity {
    companion object : UuidEntityClass<InventoryItemTypeEntity>(InventoryItemTypes)

    /**
     * Whether this single item type is visible to [session] -- must stay in sync with the `listProvider` used
     * for `GET /inventory/types` (`InventoryRoutes.kt`), which returns an *empty* list for a `null` session.
     * Evaluated directly against this entity's own department (one department lookup for the caller, not a
     * query over every item type), so this is safe to call per single-item GET.
     */
    context(_: JdbcTransaction)
    fun isVisibleTo(session: UserSession?): Boolean {
        if (session == null) return false
        val typeDepartmentId = department?.id?.value ?: return true
        return session.isAdmin() ||
            DepartmentMemberEntity.getUserDepartments(session.sub, isConfirmed = true).any { it.department.id.value == typeDepartmentId }
    }

    override var lastUpdate by InventoryItemTypes.lastUpdate

    var displayName by InventoryItemTypes.displayName
    var description by InventoryItemTypes.description
    var categories by InventoryItemTypes.categories

    var weight by InventoryItemTypes.weight

    var department by DepartmentEntity optionalReferencedOn InventoryItemTypes.department

    override var image by FileEntity optionalReferencedOn InventoryItemTypes.image

    context(_: JdbcTransaction)
    override fun toData(): InventoryItemType = InventoryItemType(
        id = id.value,
        displayName = displayName,
        description = description,
        categories = categories,
        weight = weight,
        department = department?.id?.value,
        image = image?.id?.value
    )

    context(_: JdbcTransaction)
    override fun patch(request: UpdateInventoryItemTypeRequest) {
        request.displayName?.let { displayName = it }
        request.description?.let { description = it.takeUnless { value -> value.isBlank() } }
        request.categories?.let { categories = it }
        request.weight?.let { weight = it }
        request.department?.let { department = DepartmentEntity.findById(it) }
        updateOrSetImage(request.image)
    }

    override suspend fun updated() {
        notifyUpdateForEntity(Companion, id)
        Database { lastUpdate = now() }
    }

    override fun delete() {
        val image = image
        super.delete()
        FileEntity.deleteOwnedFiles(listOfNotNull(image))
    }
}
