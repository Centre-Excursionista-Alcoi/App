package org.centrexcursionistalcoi.app.data

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.serialization.Serializable

@Serializable
data class ItemTypeD(
    override val id: Int? = null,
    val createdAt: Long? = null,
    val title: String = "",
    val description: String? = null,
    val brand: String? = null,
    val model: String? = null,
    val imageBytesBase64: String? = null,
    val sectionId: Int? = null
): DatabaseData, Validator {
    @ExperimentalEncodingApi
    constructor(
        id: Int? = null,
        createdAt: Long? = null,
        title: String = "",
        description: String? = null,
        brand: String? = null,
        model: String? = null,
        imageBytes: ByteArray,
        sectionId: Int? = null
    ): this(id, createdAt, title, description, brand, model, imageBytes.let(Base64::encode), sectionId)

    @ExperimentalEncodingApi
    fun imageBytes() = imageBytesBase64?.let(Base64::decode)

    override fun validate(): Boolean {
        return title.isNotBlank() && sectionId != null
    }
}
