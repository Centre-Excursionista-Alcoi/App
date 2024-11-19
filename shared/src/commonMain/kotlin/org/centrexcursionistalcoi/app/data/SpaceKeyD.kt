package org.centrexcursionistalcoi.app.data

import kotlinx.serialization.Serializable

@Serializable
data class SpaceKeyD(
    override val id: Int? = null,
    val createdAt: Long? = null,
    val name: String = "",
    val description: String? = null,
    val spaceId: Int? = null
): DatabaseData, Validator {
    override fun validate(): Boolean {
        return name.isNotBlank() && name.length < 255
    }
}
