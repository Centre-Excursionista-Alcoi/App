package org.centrexcursionistalcoi.app.data

import kotlinx.serialization.Serializable

@Serializable
data class CategoryPrice(
    val category: Category,
    val price: Double,
    val unit: PriceUnit = PriceUnit.PER_DAY,
)
