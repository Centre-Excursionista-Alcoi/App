package org.centrexcursionistalcoi.app.security

import org.centrexcursionistalcoi.app.database.entity.UserReferenceEntity
import org.centrexcursionistalcoi.app.exception.UserNotFoundException

/**
 * Checks if a user has a specific permission.
 * @param sub The user's unique identifier.
 * @param permission The permission to check.
 * @return `true` if the user has the permission, `false` otherwise.
 * @throws IllegalArgumentException if the permission format is invalid.
 * @throws UserNotFoundException if the user with the given sub does not exist.
 */
fun Permissions.hasPermission(sub: String, permission: String): Boolean {
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
    return Permissions.hasPermission(permission, user.groups)
}
