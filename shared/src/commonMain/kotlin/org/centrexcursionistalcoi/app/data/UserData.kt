package org.centrexcursionistalcoi.app.data

import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.ADMIN_GROUP_NAME
import org.centrexcursionistalcoi.app.exception.UserNotFoundException
import org.centrexcursionistalcoi.app.response.ProfileResponse

@Serializable
data class UserData(
    val sub: String,
    val fullName: String,
    val email: String,
    val groups: List<String>,
    val departments: List<DepartmentMemberInfo>,
    val lendingUser: LendingUser?,
    val insurances: List<UserInsurance>,
    val isDisabled: Boolean,
): Entity<String>, SubReferencedFileContainer {
    companion object {
        /**
         * Gets a [UserData] from a list by its [sub].
         * @throws UserNotFoundException if no user with the given [sub] is found
         */
        fun List<UserData>.getUser(sub: String): UserData = this.firstOrNull { it.sub == sub } ?: throw UserNotFoundException(sub)
    }

    override val id: String = sub

    fun isAdmin(): Boolean {
        return groups.contains(ADMIN_GROUP_NAME)
    }

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

    /**
     * Strips sensitive information from the user data.
     * @return A new [UserData] instance with sensitive fields removed.
     */
    fun strip(): UserData = copy(email = "\u0000", departments = emptyList(), lendingUser = null, insurances = emptyList())

    override val referencedFiles: List<Triple<String, Uuid?, String>>
        get() {
            val insurancesFiles = insurances.flatMap { it.files.entries }
                .map { Triple(it.key, it.value, UserInsurance::class.simpleName!!) }
                .toTypedArray()
            return listOf(*insurancesFiles)
        }
}
