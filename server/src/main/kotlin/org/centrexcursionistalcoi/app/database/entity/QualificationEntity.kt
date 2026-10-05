package org.centrexcursionistalcoi.app.database.entity

import kotlin.uuid.Uuid
import org.centrexcursionistalcoi.app.data.Qualification
import org.centrexcursionistalcoi.app.database.table.Qualifications
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UuidEntity
import org.jetbrains.exposed.v1.dao.UuidEntityClass

class QualificationEntity(id: EntityID<Uuid>) : UuidEntity(id) {
    companion object : UuidEntityClass<QualificationEntity>(Qualifications)

    var department by DepartmentEntity referencedOn Qualifications.department
    var name by Qualifications.name
    var description by Qualifications.description

    fun toData() = Qualification(
        id = id.value,
        departmentId = Qualifications.department.lookup().value,
        name = name,
        description = description,
    )
}
