package org.centrexcursionistalcoi.app.database.entity

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey
import org.centrexcursionistalcoi.app.data.DepartmentMemberInfo
import org.centrexcursionistalcoi.app.data.LendingUser
import org.centrexcursionistalcoi.app.data.UserData
import org.centrexcursionistalcoi.app.data.UserInsurance

@Entity(
    tableName = "Users",
    indices = [Index(value = ["isDisabled"], name = "idx_users_isDisabled")],
)
data class UserEntity(
    @PrimaryKey
    val sub: String,
    val memberNumber: Long,
    val fullName: String,
    val email: String,
    val groups: List<String>,
    val departments: List<DepartmentMemberInfo>,
    val lendingUser: LendingUser?,
    val insurances: List<UserInsurance>,
    val isDisabled: Boolean,
) {
    fun toUser() = UserData(
        sub = sub,
        memberNumber = memberNumber.toUInt(),
        fullName = fullName,
        email = email,
        groups = groups,
        departments = departments,
        lendingUser = lendingUser,
        insurances = insurances,
        isDisabled = isDisabled,
    )

    companion object {
        fun UserData.toEntity() = UserEntity(
            sub = sub,
            memberNumber = memberNumber.toLong(),
            fullName = fullName,
            email = email,
            groups = groups,
            departments = departments,
            lendingUser = lendingUser,
            insurances = insurances,
            isDisabled = isDisabled,
        )
    }
}
