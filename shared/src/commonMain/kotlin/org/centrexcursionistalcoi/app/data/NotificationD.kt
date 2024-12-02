package org.centrexcursionistalcoi.app.data

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.data.enumeration.NotificationType

@Serializable
data class NotificationD(
    override val id: Int,
    val createdAt: Instant,
    val viewed: Boolean,
    val type: NotificationType,
    val payload: String,
    val userId: String
): DatabaseData
