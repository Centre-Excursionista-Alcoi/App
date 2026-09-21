package org.centrexcursionistalcoi.app.database.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import kotlin.uuid.Uuid
import org.centrexcursionistalcoi.app.data.Qualification

/**
 * A qualification definition. No foreign key to the department: the list is replaced wholesale on every sync, and
 * a definition must never block (or be dragged along by) changes to departments.
 */
@Entity(tableName = "Qualifications")
data class QualificationEntity(
    @PrimaryKey
    val id: Uuid,
    val departmentId: Uuid,
    val name: String,
    val description: String?,
) {
    fun toQualification() = Qualification(id, departmentId, name, description)

    companion object {
        fun Qualification.toEntity() = QualificationEntity(id, departmentId, name, description)
    }
}
