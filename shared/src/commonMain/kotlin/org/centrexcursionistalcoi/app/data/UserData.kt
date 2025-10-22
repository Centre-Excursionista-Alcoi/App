package org.centrexcursionistalcoi.app.data

import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.response.ProfileResponse

@Serializable
data class UserData(
    val sub: String,
    val username: String,
    val email: String,
    val groups: List<String>,
    val departments: List<DepartmentMemberInfo>,
    val lendingUser: LendingUser?,
    val insurances: List<UserInsurance>
): Entity<String> {
    override val id: String = sub

    override fun toMap(): Map<String, Any?> {
        throw UnsupportedOperationException("UserData cannot be converted to a Map. This is a read-only entity.")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other is ProfileResponse) return other.sub == sub
        if (other == null || this::class != other::class) return false

        other as UserData

        return sub == other.sub
    }

    override fun hashCode(): Int {
        return sub.hashCode()
    }
}
