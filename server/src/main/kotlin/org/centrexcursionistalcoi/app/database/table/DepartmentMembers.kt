package org.centrexcursionistalcoi.app.database.table

import org.jetbrains.exposed.v1.core.dao.id.IntIdTable

object DepartmentMembers : IntIdTable("department_members") {
    /** The Subject Identifier of the user */
    val userSub = text("sub")
    val departmentId = reference("department_id", Departments)

    val confirmed = bool("confirmed").default(false)

    init {
        // A user can only be member of a department once
        uniqueIndex(userSub, departmentId)
    }
}
