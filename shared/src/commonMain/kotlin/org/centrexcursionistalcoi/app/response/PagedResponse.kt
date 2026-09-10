package org.centrexcursionistalcoi.app.response

import kotlinx.serialization.Serializable

/**
 * A page of results from a searchable/paginated list endpoint.
 */
@Serializable
data class PagedResponse<T>(
    val items: List<T>,
    val total: Long,
    val limit: Int,
    val offset: Int,
)
