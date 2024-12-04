package org.centrexcursionistalcoi.app.push.payload

import kotlinx.serialization.Serializable

@Serializable
sealed interface AdminPayload: PushPayload {
    val type: AdminNotificationType
    val rawData: String
}
