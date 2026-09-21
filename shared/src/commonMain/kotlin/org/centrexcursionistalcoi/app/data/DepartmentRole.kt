package org.centrexcursionistalcoi.app.data

import kotlinx.serialization.Serializable

/**
 * A permission a user can hold within a single department (via a confirmed [DepartmentMemberInfo]).
 *
 * [ADMIN] implies every other role: any permission check for another role must also pass for a member whose
 * roles include [ADMIN]. [QUALIFICATIONS_MANAGER] additionally implies [EXAMINER]. See [implies].
 */
@Serializable
enum class DepartmentRole {
    ADMIN,
    PEOPLE_MANAGER,
    INVENTORY_MANAGER,
    LENDING_MANAGER,
    MEMORY_MANAGER,
    CONTENT_MANAGER,

    /** Creates, edits and deletes the department's qualification definitions. Implies [EXAMINER]. */
    QUALIFICATIONS_MANAGER,

    /** Grants and revokes the department's qualifications to/from its members. */
    EXAMINER;

    /**
     * `true` if holding this role also satisfies a permission check for [other]: every role implies itself,
     * [ADMIN] implies every role, and [QUALIFICATIONS_MANAGER] implies [EXAMINER].
     */
    fun implies(other: DepartmentRole): Boolean =
        this == other || this == ADMIN || (this == QUALIFICATIONS_MANAGER && other == EXAMINER)

    /** The lowercase snake_case form persisted in the `department_members.roles` array column. */
    val storageName: String get() = name.lowercase()

    companion object {
        fun fromStorageName(value: String): DepartmentRole? = entries.firstOrNull { it.storageName == value }
    }
}
