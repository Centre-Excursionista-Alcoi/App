package org.centrexcursionistalcoi.app.database.entity

import kotlin.uuid.toKotlinUuid
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.database.table.Departments
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction

class DepartmentEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<DepartmentEntity>(Departments)

    var displayName by Departments.displayName
    var imageFile by Departments.imageFile

    context(_: JdbcTransaction)
    fun toData(): Department = Department(
        id = id.value.toLong(),
        displayName = displayName,
        imageFile = imageFile?.value?.toKotlinUuid()
    )
}
