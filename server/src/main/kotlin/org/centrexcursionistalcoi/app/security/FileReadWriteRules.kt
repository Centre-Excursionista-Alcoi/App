package org.centrexcursionistalcoi.app.security

import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.plugins.UserSession

@Serializable
data class FileReadWriteRules(
    val writeUsers: List<String>? = null,
    val readUsers: List<String>? = null,
    val writeGroups: List<String>? = null,
    val readGroups: List<String>? = null,
) {
    /**
     * Checks whether a user can read the file based on their user ID and groups.
     *
     * - If [userId] is null, it represents an unauthenticated user.
     * - If [userGroups] is null or empty, it represents a user with no groups.
     */
    fun canBeReadBy(userId: String?, userGroups: List<String>?): Boolean {
        if (isPublic()) return true
        if (readUsers != null && userId == null) return false
        if (readUsers != null && userId != null && readUsers.contains(userId)) return true
        if (readGroups != null && userGroups != null && userGroups.any { readGroups.contains(it) }) return true
        return false
    }

    fun canBeReadBy(userSession: UserSession?) = canBeReadBy(userSession?.sub, userSession?.groups)

    /**
     * Returns `true` if the file is public (i.e., no restrictions on reading or writing).
     */
    fun isPublic() = writeUsers == null && readUsers == null && writeGroups == null && readGroups == null
}
