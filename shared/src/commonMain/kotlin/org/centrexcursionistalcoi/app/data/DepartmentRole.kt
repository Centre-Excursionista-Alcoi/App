package org.centrexcursionistalcoi.app.data

import kotlinx.serialization.Serializable

/**
 * A permission a user can hold within a single department (via a confirmed [DepartmentMemberInfo]).
 *
 * [ADMIN] implies every other role: any permission check for another role must also pass for a member whose
 * roles include [ADMIN].
 */
@Serializable
enum class DepartmentRole {
    ADMIN,
    PEOPLE_MANAGER,
    INVENTORY_MANAGER,
    LENDING_MANAGER,
    MEMORY_MANAGER,
    CONTENT_MANAGER;

    /** The lowercase snake_case form persisted in the `department_members.roles` array column. */
    val storageName: String get() = name.lowercase()

    companion object {
        fun fromStorageName(value: String): DepartmentRole? = entries.firstOrNull { it.storageName == value }
    }
}
