package org.centrexcursionistalcoi.app.notifications.email

import io.mockk.every
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import io.mockk.verify
import javax.mail.Message
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import javax.mail.util.ByteArrayDataSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.centrexcursionistalcoi.app.notifications.NotificationsConfig
import org.centrexcursionistalcoi.app.notifications.email.mailersend.MailerSendAttachment
import org.centrexcursionistalcoi.app.notifications.email.mailersend.MailerSendEmail

class TestSmtpProvider {

    @Test
    fun `isConfigured reflects NotificationsConfig and sendEmail fails when not configured`() {
        mockkObject(NotificationsConfig)
        try {
            // Make sure SMTP is not configured
            every { NotificationsConfig.smtpHost } returns null
            every { NotificationsConfig.smtpPort } returns null
            every { NotificationsConfig.smtpUsername } returns null
            every { NotificationsConfig.smtpPassword } returns null

            assertFalse(SmtpProvider.isConfigured)

            // Attempting to send should throw because SmtpProvider.check(isConfigured) fails
            val to = listOf(MailerSendEmail("to@example.com", "To Name"))
            runBlocking {
                assertFailsWith<IllegalStateException> {
                    SmtpProvider.sendEmail(to, "subject", "<b>hi</b>", null)
                }
            }
        } finally {
            unmockkObject(NotificationsConfig)
        }
    }

    @Test
    fun `sendEmail builds MimeMessage with headers, body and attachments`() {
        mockkObject(NotificationsConfig)
        mockkStatic(Transport::class)
        val msgSlot = slot<Message>()

        try {
            // Provide SMTP config
            every { NotificationsConfig.smtpHost } returns "smtp.example.com"
            every { NotificationsConfig.smtpPort } returns 587
            every { NotificationsConfig.smtpUsername } returns "user"
            every { NotificationsConfig.smtpPassword } returns "pass"
            every { NotificationsConfig.smtpUseTls } returns false

            // From / ReplyTo defaults
            every { NotificationsConfig.emailFromAddr } returns "noreply@test.org"
            every { NotificationsConfig.emailFromName } returns "No Reply"
            every { NotificationsConfig.emailReplyToAddr } returns "reply@test.org"
            every { NotificationsConfig.emailReplyToName } returns "Reply Name"

            // Capture Transport.send calls
            every { Transport.send(capture(msgSlot)) } answers { /* do nothing */ }

            val to = listOf(MailerSendEmail("recipient@example.com", "Recipient Name"))
            val subject = "Test subject"
            val html = "<p>Hello <strong>world</strong></p>"

            val attachmentBytes = "hello-attachment".toByteArray()
            val attachment = MailerSendAttachment(attachmentBytes, "greeting.txt")

            runBlocking {
                SmtpProvider.sendEmail(to, subject, html, listOf(attachment))
            }

            // Verify Transport.send invoked
            verify(exactly = 1) { Transport.send(any()) }

            val sent = msgSlot.captured
            // Basic checks on message
            require(sent is MimeMessage)

            // From
            val from = sent.from?.firstOrNull() as? InternetAddress
            assertEquals("noreply@test.org", from?.address)
            assertEquals("No Reply", from?.personal)

            // Reply-To
            val replyTo = sent.replyTo?.firstOrNull() as? InternetAddress
            assertEquals("reply@test.org", replyTo?.address)
            assertEquals("Reply Name", replyTo?.personal)

            // Subject
            assertEquals(subject, sent.subject)

            // Recipients
            val toAddr = sent.getRecipients(Message.RecipientType.TO)?.firstOrNull() as? InternetAddress
            assertEquals("recipient@example.com", toAddr?.address)
            assertEquals("Recipient Name", toAddr?.personal)

            // Body and attachment
            val content = sent.content
            if (content is javax.mail.Multipart) {
                // First part should be the HTML body
                val bodyPart = content.getBodyPart(0)
                val bodyContent = bodyPart.content.toString()
                assertTrue(bodyContent.contains("Hello"))

                // Second part should be the attachment
                val attachmentPart = content.getBodyPart(1)
                assertEquals("greeting.txt", attachmentPart.fileName)
                assertEquals(javax.mail.Part.ATTACHMENT, attachmentPart.disposition)

                val ds = attachmentPart.dataHandler.dataSource
                // Try to read bytes if it's a ByteArrayDataSource
                if (ds is ByteArrayDataSource) {
                    val read = ds.inputStream.readBytes()
                    assertEquals(attachmentBytes.contentToString(), read.contentToString())
                }
            } else {
                // If no multipart, at least check the content contains the HTML
                assertTrue(content.toString().contains("Hello"))
            }

        } finally {
            unmockkStatic(Transport::class)
            unmockkObject(NotificationsConfig)
        }
    }
}
