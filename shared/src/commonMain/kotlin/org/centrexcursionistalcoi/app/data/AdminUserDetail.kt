package org.centrexcursionistalcoi.app.data

import kotlinx.serialization.Serializable

/**
 * Everything the admin user detail page shows about one user: their full profile plus their entire lending
 * and memory history.
 */
@Serializable
data class AdminUserDetail(
    val profile: UserData,
    val lendings: List<AdminLendingSummary>,
    val memories: List<AdminMemorySummary>,
)
