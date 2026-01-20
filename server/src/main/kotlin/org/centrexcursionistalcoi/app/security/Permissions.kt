package org.centrexcursionistalcoi.app.security

import org.centrexcursionistalcoi.app.ADMIN_GROUP_NAME
import org.centrexcursionistalcoi.app.database.entity.UserReferenceEntity
import org.centrexcursionistalcoi.app.exception.UserNotFoundException
import org.centrexcursionistalcoi.app.security.ParametrizedPermission.Companion.parametrized
import org.jetbrains.annotations.VisibleForTesting

object Permissions {
    /**
     * Validates the format of a permission string.
     * Permissions can be only: lowercase, with '_' and '.'. Wildcards '*' are allowed, and may replace any characters between dots.
     * Example: "user.view", "user.*", "user.*.edit".
     * @return `true` if the permission format is valid, `false` otherwise.
     */
    @VisibleForTesting
    fun validatePermissionFormat(permission: String): Boolean {
        val permissionRegex = Regex("^[a-z0-9_]+(\\.[a-z0-9_*]+)*$")
        return permissionRegex.matches(permission)
    }

    /**
     * Checks if a user has a specific permission.
     * @param sub The user's unique identifier.
     * @param permission The permission to check.
     * @return `true` if the user has the permission, `false` otherwise.
     * @throws IllegalArgumentException if the permission format is invalid.
     * @throws UserNotFoundException if the user with the given sub does not exist.
     */
    fun hasPermission(sub: String, permission: String): Boolean {
        // Validate permission format
        require(validatePermissionFormat(permission)) {
            "Invalid permission format: $permission"
        }

        // Fetch user by sub
        val user = UserReferenceEntity.findById(sub) ?: throw UserNotFoundException("User with sub $sub not found")

        // Disabled users have no permissions
        if (user.isDisabled) {
            return false
        }

        // Check whether the user has the permission
        return hasPermission(permission, user.groups)
    }

    fun hasPermission(permission: String, groups: List<String>): Boolean {
        if (groups.contains(ADMIN_GROUP_NAME)) {
            return true
        }

        // Check if user has the specific permission (is in the group)
        if (groups.contains(permission)) {
            return true
        }

        for (group in groups) {
            // Check if user has wildcard permission: replace '*' with regex '.*', and '.' with '\.' (escape dot)
            val groupRegex = Regex("^" + group.replace(".", "\\.").replace("*", ".*") + "$")
            if (groupRegex.matches(permission)) {
                return true
            }
        }

        // Permission not found
        return false
    }

    object Department {
        /** Allows kicking any user from the department. */
        val KICK = "department.*.kick".parametrized()

        /** Allows managing join requests from a user for a department. */
        val MANAGE_REQUESTS = "department.*.manage_requests".parametrized()
    }
}
