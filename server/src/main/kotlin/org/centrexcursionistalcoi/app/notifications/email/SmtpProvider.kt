package org.centrexcursionistalcoi.app.notifications.email

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import jakarta.activation.DataHandler
import jakarta.mail.Authenticator
import jakarta.mail.PasswordAuthentication
import jakarta.mail.Session
import jakarta.mail.Transport
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeBodyPart
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimeMultipart
import jakarta.mail.util.ByteArrayDataSource
import java.util.Date
import java.util.Properties
import org.centrexcursionistalcoi.app.notifications.NotificationsConfig
import org.centrexcursionistalcoi.app.notifications.email.mailersend.MailerSendAttachment
import org.centrexcursionistalcoi.app.notifications.email.mailersend.MailerSendEmail
import org.centrexcursionistalcoi.app.now
import org.slf4j.LoggerFactory

object SmtpProvider : EmailProvider {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override val isConfigured: Boolean
        get() = NotificationsConfig.smtpHost != null &&
                NotificationsConfig.smtpPort != null &&
                NotificationsConfig.smtpUsername != null &&
                NotificationsConfig.smtpPassword != null

    /**
     * Sends an email using SMTP, with the credentials configured in [NotificationsConfig].
     * @throws IllegalStateException if SMTP is not configured.
     * @param to List of recipient email addresses.
     * @param subject Subject of the email.
     * @param htmlContent HTML content of the email.
     * @param attachments Optional list of attachments.
     */
    override suspend fun sendEmail(
        to: List<MailerSendEmail>,
        subject: String,
        htmlContent: String,
        attachments: List<MailerSendAttachment>?
    ) {
        check(isConfigured) { "SMTP is not configured." }

        logger.debug("Setting up SMTP session...")
        val props = Properties().apply {
            put("mail.smtp.auth", "true")
            put("mail.smtp.host", NotificationsConfig.smtpHost)
            put("mail.smtp.port", NotificationsConfig.smtpPort.toString())

            if (NotificationsConfig.smtpUseTls) {
                put("mail.smtp.starttls.enable", true)
                put("mail.smtp.starttls.required", true)
            }
        }

        val auth = object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(
                    NotificationsConfig.smtpUsername,
                    NotificationsConfig.smtpPassword
                )
            }
        }
        logger.debug("Creating SMTP session...")
        val session = Session.getInstance(props, auth)
        sendEmail(session, to, subject, htmlContent, attachments)
    }

    private fun sendEmail(
        session: Session,
        to: List<MailerSendEmail>,
        subject: String,
        htmlContent: String,
        attachments: List<MailerSendAttachment>?
    ) {
        logger.debug("Creating email message...")
        val msg = MimeMessage(session).apply {
            logger.debug("Preparing email to ${to.joinToString { it.email }} with subject '$subject'")
            addHeader(HttpHeaders.ContentType, "text/HTML; charset=UTF-8")
            addHeader("format", "flowed")
            addHeader("Content-Transfer-Encoding", "8bit")
            sentDate = Date.from(now())

            val from = InternetAddress(NotificationsConfig.emailFromAddr, NotificationsConfig.emailFromName)
            setFrom(from)
            sender = from

            if (NotificationsConfig.emailReplyToAddr != null) {
                replyTo = arrayOf(
                    InternetAddress(
                        NotificationsConfig.emailReplyToAddr,
                        NotificationsConfig.emailReplyToName
                    )
                )
            }

            setSubject(subject)

            if (attachments.isNullOrEmpty()) {
                setContent(htmlContent, "text/html; charset=UTF-8")
            } else {
                val messageBodyPart = MimeBodyPart().apply {
                    setContent(htmlContent, "text/html; charset=UTF-8")
                }

                val multipart = MimeMultipart().apply {
                    // Add the message body to the multipart
                    addBodyPart(messageBodyPart)

                    // Add all the attachments
                    for (attachment in attachments) {
                        val attachmentBodyPart = MimeBodyPart().apply {
                            fileName = attachment.filename
                            disposition = attachment.disposition
                            dataHandler = DataHandler(
                                ByteArrayDataSource(attachment.decodeContent(), ContentType.Application.OctetStream.toString())
                            )
                        }
                        addBodyPart(attachmentBodyPart)
                    }
                }
                setContent(multipart)
            }

            for (recipient in to) {
                addRecipient(MimeMessage.RecipientType.TO, InternetAddress(recipient.email, recipient.name))
            }
        }

        logger.debug("Sending email via SMTP...")
        Transport.send(msg)

        logger.info("Email sent to ${to.joinToString { it.email }} with subject '$subject'")
    }
}
