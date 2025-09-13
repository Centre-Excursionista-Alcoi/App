package org.centrexcursionistalcoi.app.database.entity

import org.centrexcursionistalcoi.app.database.table.Departments
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass

class Department(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Department>(Departments)

    var displayName by Departments.displayName
}
