package org.centrexcursionistalcoi.app.request

import kotlinx.serialization.Serializable

@Serializable
data class DeleteLendingRequest(
    val message: String?
)
