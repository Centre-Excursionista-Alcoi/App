package org.centrexcursionistalcoi.app.data

import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.data.enumeration.NotificationType
import org.centrexcursionistalcoi.app.push.payload.PushPayload

@Serializable
data class NotificationD(
    override val id: Int? = null,
    val createdAt: Long? = null,
    val viewed: Boolean,
    val type: NotificationType,
    val payload: PushPayload,
    val userId: String
): DatabaseData
