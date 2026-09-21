package org.centrexcursionistalcoi.app.database.table

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable

/** A qualification defined by a department, see [org.centrexcursionistalcoi.app.data.Qualification]. */
object Qualifications : UUIDTable("qualifications") {
    val department = reference("department_id", Departments, ReferenceOption.CASCADE, ReferenceOption.RESTRICT)

    val name = varchar("name", 255)
    val description = text("description").nullable()

    init {
        // A department can't define two qualifications with the same name
        uniqueIndex(department, name)
    }
}
