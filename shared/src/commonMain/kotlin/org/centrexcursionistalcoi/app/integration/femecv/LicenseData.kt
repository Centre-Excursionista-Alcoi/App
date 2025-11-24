package org.centrexcursionistalcoi.app.integration.femecv

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class LicenseData(
    val code: String,
    val id: Int,
    val club: String,
    val modalityId: Int,
    val modalityName: String?,
    val categoryId: Int,
    val subCategoryId: Int,
    val validFrom: LocalDate,
    val validTo: LocalDate,
    val imageUrl: String?,
)
