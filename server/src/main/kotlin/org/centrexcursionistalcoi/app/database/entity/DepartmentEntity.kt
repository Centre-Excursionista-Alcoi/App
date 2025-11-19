package org.centrexcursionistalcoi.app.database.entity

import java.util.UUID
import kotlin.uuid.Uuid
import kotlin.uuid.toKotlinUuid
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.database.base.EntityPatcher
import org.centrexcursionistalcoi.app.database.entity.base.ImageContainerEntity
import org.centrexcursionistalcoi.app.database.table.Departments
import org.centrexcursionistalcoi.app.request.UpdateDepartmentRequest
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction

class DepartmentEntity(id: EntityID<UUID>) : UUIDEntity(id), EntityDataConverter<Department, Uuid>, EntityPatcher<UpdateDepartmentRequest>, ImageContainerEntity {
    companion object : UUIDEntityClass<DepartmentEntity>(Departments)

    var displayName by Departments.displayName
    override var image by FileEntity optionalReferencedOn Departments.image

    context(_: JdbcTransaction)
    override fun toData(): Department = Department(
        id = id.value.toKotlinUuid(),
        displayName = displayName,
        image = image?.id?.value?.toKotlinUuid()
    )

    context(_: JdbcTransaction)
    override fun patch(request: UpdateDepartmentRequest) {
        request.displayName?.let { displayName = it }
        updateOrSetImage(request.image, "department_${id.value}_image")
    }
}
