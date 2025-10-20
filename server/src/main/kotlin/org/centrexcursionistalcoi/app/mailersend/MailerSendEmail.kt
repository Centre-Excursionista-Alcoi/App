package org.centrexcursionistalcoi.app.mailersend

import kotlinx.serialization.Serializable

@Serializable
data class MailerSendEmail(
    val email: String,
    val name: String,
)
