package org.centrexcursionistalcoi.app.database.entity

import java.util.UUID
import kotlin.uuid.toKotlinUuid
import org.centrexcursionistalcoi.app.data.Qualification
import org.centrexcursionistalcoi.app.database.table.Qualifications
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.java.UUIDEntity
import org.jetbrains.exposed.v1.dao.java.UUIDEntityClass

class QualificationEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<QualificationEntity>(Qualifications)

    var department by DepartmentEntity referencedOn Qualifications.department
    var name by Qualifications.name
    var description by Qualifications.description

    fun toData() = Qualification(
        id = id.value.toKotlinUuid(),
        departmentId = department.id.value.toKotlinUuid(),
        name = name,
        description = description,
    )
}
