package org.centrexcursionistalcoi.app.mailersend

import kotlinx.serialization.Serializable

@Serializable
data class SendMailRequest(
    val from: MailerSendEmail,
    val to: List<MailerSendEmail>,
    val subject: String,
    val html: String? = null,
    val text: String? = null,
    val attachments: List<MailerSendAttachment>? = null,
    val cc: List<MailerSendEmail>? = null,
    val bcc: List<MailerSendEmail>? = null,
    val replyTo: MailerSendEmail? = null,
) {
    init {
        require(text != null || html != null) {
            "Either text or html must be provided"
        }
        require(text == null && html != null || text != null && html == null) {
            "Only one of text or html can be provided"
        }
    }
}
