package org.centrexcursionistalcoi.app.data

import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.centrexcursionistalcoi.app.exception.DepartmentNotFoundException
import org.centrexcursionistalcoi.app.serializer.NullableUUIDSerializer

@Serializable
data class Department(
    override val id: Uuid,
    val displayName: String,
    @Serializable(NullableUUIDSerializer::class) override val image: Uuid? = null,
    val members: List<DepartmentMemberInfo>,
) : Entity<Uuid>, ImageFileContainer {
    companion object {
        /**
         * Gets a [Department] from a list by its [id].
         * @throws DepartmentNotFoundException if no department with the given [id] is found
         */
        fun List<Department>.getDepartment(id: Uuid): Department = this.firstOrNull { it.id == id } ?: throw DepartmentNotFoundException(id)
    }

    @Transient
    override val files: Map<String, Uuid?> = mapOf("image" to image)

    override fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "displayName" to displayName,
        "image" to image?.let { FileReference(it) },
        "members" to members.map { it.toMap() },
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Department) return false

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return displayName
    }
}
