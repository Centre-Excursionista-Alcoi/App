package org.centrexcursionistalcoi.app.authentik

import kotlinx.serialization.Serializable

@Serializable
data class AuthentikPaginatedResults<ItemType: AuthentikData>(
    val pagination: AuthentikPagination,
    val results: List<ItemType>,
): AuthentikData
