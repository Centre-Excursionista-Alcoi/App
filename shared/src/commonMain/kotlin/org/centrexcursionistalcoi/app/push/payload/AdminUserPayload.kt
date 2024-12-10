package org.centrexcursionistalcoi.app.push.payload

import kotlinx.serialization.Serializable

@Serializable
class AdminUserPayload(
    override val type: AdminNotificationType,

    /**
     * The user ID of the user.
     */
    override val rawData: String
) : AdminPayload {
    val userId: String get() = rawData
}
