package org.centrexcursionistalcoi.app.notifications.email

import org.centrexcursionistalcoi.app.notifications.email.mailersend.MailerSendAttachment
import org.centrexcursionistalcoi.app.notifications.email.mailersend.MailerSendEmail

sealed interface EmailProvider {
    companion object {
        val providers = listOf<EmailProvider>(
            MailerSend, SmtpProvider
        )
    }

    /**
     * Indicates whether the email provider is properly configured.
     */
    val isConfigured: Boolean

    /**
     * Verifies that the email provider is actually available as well as configured ([isConfigured]).
     * @return True if the email provider is available, false otherwise.
     */
    suspend fun isAvailable(): Boolean = true

    /**
     * Sends an email to the specified recipients.
     * @throws IllegalArgumentException If the email could not be sent because some parameters are invalid.
     * @throws IllegalStateException If the email could not be sent because of a server error.
     */
    suspend fun sendEmail(to: List<MailerSendEmail>, subject: String, htmlContent: String, attachments: List<MailerSendAttachment>? = null)
}
