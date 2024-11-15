package org.centrexcursionistalcoi.app.server.response.data

import kotlinx.serialization.Serializable

@Serializable
data class SectionD(
    override val id: Int? = null,
    val createdAt: Long? = null,
    val displayName: String = ""
): DatabaseData, Validator {
    override fun validate(): Boolean {
        return displayName.isNotBlank()
    }
}
