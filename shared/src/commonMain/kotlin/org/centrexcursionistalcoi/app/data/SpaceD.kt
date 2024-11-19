package org.centrexcursionistalcoi.app.data

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
    val images: List<String>? = null,
    val keys: List<SpaceKeyD>? = null
): DatabaseData, Validator {
    override fun validate(): Boolean = name.isNotBlank()
}
