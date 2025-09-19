package org.centrexcursionistalcoi.app.database.entity

import org.centrexcursionistalcoi.app.database.table.Departments
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass

class DepartmentEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<DepartmentEntity>(Departments)

    var displayName by Departments.displayName
    var imageFile by Departments.imageFile
}
