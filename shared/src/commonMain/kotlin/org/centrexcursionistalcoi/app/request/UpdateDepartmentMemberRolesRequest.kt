package org.centrexcursionistalcoi.app.request

import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.data.DepartmentRole

/**
 * Request body for `PATCH /departments/{id}/members/{memberId}/roles`, replacing the target member's full set of
 * [DepartmentRole]s within that department.
 */
@Serializable
data class UpdateDepartmentMemberRolesRequest(val roles: List<DepartmentRole>)
