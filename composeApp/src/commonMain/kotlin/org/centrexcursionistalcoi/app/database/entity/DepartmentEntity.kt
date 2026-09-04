package org.centrexcursionistalcoi.app.database.entity

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.data.DepartmentMemberInfo
import kotlin.uuid.Uuid

@Entity(
    tableName = "Departments",
    indices = [Index(value = ["displayName"], name = "idx_Departments_displayName")],
)
data class DepartmentEntity(
    @PrimaryKey
    val id: Uuid,
    val displayName: String,
    val imageFile: Uuid? = null,
    val members: List<DepartmentMemberInfo>?,
) {
    fun toDepartment() = Department(
        id = id,
        displayName = displayName,
        image = imageFile,
        members = members,
    )

    companion object {
        fun Department.toEntity() = DepartmentEntity(
            id = id,
            displayName = displayName,
            imageFile = image,
            members = members,
        )
    }
}
