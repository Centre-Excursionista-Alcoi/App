package backend.data.app

import kotlinx.serialization.Serializable

@Serializable
data class StockInfo(
    val available: ULong,
    val inUse: ULong,
    val reserved: ULong
)
