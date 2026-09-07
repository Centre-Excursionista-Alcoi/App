package org.centrexcursionistalcoi.app.data

import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.centrexcursionistalcoi.app.exception.DepartmentNotFoundException
import org.centrexcursionistalcoi.app.response.ProfileResponse
import org.centrexcursionistalcoi.app.serializer.NullableUUIDSerializer

@Serializable
data class Department(
    override val id: Uuid,
    val displayName: String,
    @Serializable(NullableUUIDSerializer::class) override val image: Uuid? = null,
    val members: List<DepartmentMemberInfo>?,
) : Entity<Uuid>, ImageFileContainer {
    companion object {
        /**
         * Gets a [Department] from a list by its [id].
         * @throws DepartmentNotFoundException if no department with the given [id] is found
         */
        fun List<Department>.getDepartment(id: Uuid): Department = this.firstOrNull { it.id == id } ?: throw DepartmentNotFoundException(id)

        /**
         * Checks if the profile holds any role (of any kind) in any department in the list.
         */
        fun List<Department>.isManagerOfAny(profile: ProfileResponse): Boolean {
            return this.any { department ->
                department.members.orEmpty().find { it.userSub == profile.sub && it.confirmed }?.roles?.isNotEmpty() == true
            }
        }

        /**
         * Filters this list down to the departments where the profile holds [role] (or [DepartmentRole.ADMIN],
         * which implies every role) as a confirmed member. Mirrors the server's per-department authorization: use
         * this to decide which departments to offer as a destination, or which items a management UI should let
         * the profile edit/delete.
         */
        fun List<Department>.departmentsWithRole(profile: ProfileResponse, role: DepartmentRole): List<Department> {
            return filter { department ->
                department.members.orEmpty().find { it.userSub == profile.sub && it.confirmed }?.roles?.let {
                    role in it || DepartmentRole.ADMIN in it
                } == true
            }
        }

        /**
         * Checks if the profile holds the given [role] (or [DepartmentRole.ADMIN], which implies every role) in
         * any department in the list.
         */
        fun List<Department>.hasAnyDepartmentRole(profile: ProfileResponse, role: DepartmentRole): Boolean {
            return departmentsWithRole(profile, role).isNotEmpty()
        }
    }

    @Transient
    override val files: Map<String, Uuid?> = mapOf("image" to image)

    override fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "displayName" to displayName,
        "image" to image?.let { FileReference(it) },
        "members" to members?.map { it.toMap() },
    )

    override fun toString(): String {
        return displayName
    }
}
