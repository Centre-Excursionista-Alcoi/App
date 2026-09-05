package org.centrexcursionistalcoi.app.data

import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable

@Serializable
data class DepartmentMemberInfo(
    override val id: Uuid,
    val userSub: String,
    val departmentId: Uuid,
    val confirmed: Boolean,
    val roles: List<DepartmentRole> = emptyList(),
): Entity<Uuid> {
    override fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "userSub" to userSub,
        "departmentId" to departmentId,
        "confirmed" to confirmed,
        "roles" to roles,
    )
}
