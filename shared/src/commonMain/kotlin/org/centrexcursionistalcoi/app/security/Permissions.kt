package org.centrexcursionistalcoi.app.security

import org.centrexcursionistalcoi.app.ADMIN_GROUP_NAME
import org.centrexcursionistalcoi.app.security.ParametrizedPermission.Companion.parametrized


object Permissions {
    /**
     * Validates the format of a permission string.
     * Permissions can be only: lowercase, with '_', '-' and '.'. Wildcards '*' are allowed, and may replace any characters between dots.
     * Example: "user.view", "user.*", "user.*.edit".
     * @return `true` if the permission format is valid, `false` otherwise.
     */
    fun validatePermissionFormat(permission: String): Boolean {
        val permissionRegex = Regex("^[a-z0-9_-]+(\\.[a-z0-9_\\-*]+)*$")
        return permissionRegex.matches(permission)
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

    object Lending {
        /** Allows giving items from any department. */
        const val GIVE = "lending.give"

        /** Allows giving items linked to a department (placeholder). */
        val GIVE_BY_DEPARTMENT = "lending.give.by_department.*".parametrized()

        /** Allows giving items not linked to any department. */
        const val GIVE_NO_DEPARTMENT = "lending.give.by_department.none"

        /** Allows receiving items from any department. */
        const val RECEIVE = "lending.receive"

        /** Allows receiving items linked to a department (placeholder). */
        val RECEIVE_BY_DEPARTMENT = "lending.receive.by_department.*".parametrized()

        /** Allows receiving items not linked to any department. */
        const val RECEIVE_NO_DEPARTMENT = "lending.receive.by_department.none"
    }
}
