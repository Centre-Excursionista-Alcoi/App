package org.centrexcursionistalcoi.app.server.response.data

import kotlinx.serialization.Serializable

@Serializable
data class SpaceD(
    override val id: Int? = null,
    val createdAt: Long? = null,
    val name: String = "",
    val description: String? = null,
    val capacity: Int? = null,
    val memberPrice: MoneyD? = null,
    val externalPrice: MoneyD? = null,
    val location: Location? = null,
    val address: Address? = null,
): DatabaseData
