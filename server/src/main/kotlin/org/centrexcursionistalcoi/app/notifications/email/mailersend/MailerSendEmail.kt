package org.centrexcursionistalcoi.app.notifications.email.mailersend

import kotlinx.serialization.Serializable

@Serializable
data class MailerSendEmail(
    val email: String,
    val name: String,
)
