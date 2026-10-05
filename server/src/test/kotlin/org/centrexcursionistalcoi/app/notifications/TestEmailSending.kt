package org.centrexcursionistalcoi.app.notifications

import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import org.centrexcursionistalcoi.app.notifications.email.mailersend.MailerSendEmail

@OptIn(ExperimentalXmlUtilApi::class)
class TestEmailSending {
    @BeforeTest
    fun record() {
        Email.sent = mutableListOf()
    }

    @AfterTest
    fun stopRecording() {
        Email.sent = null
    }

    private fun recipient(email: String, language: String) =
        EmailRecipient(MailerSendEmail(email, email.substringBefore('@')), Locale.forLanguageTag(language))

    @Test
    fun `each recipient gets the email in their language`() = runTest {
        Email.sendTemplate(
            recipients = listOf(recipient("a@example.com", "ca"), recipient("b@example.com", "es"), recipient("c@example.com", "ca")),
            template = EmailTemplate.NewSpaceLending,
            args = mapOf("id" to "7", "userName" to "Alice", "spaceName" to "Casa", "link" to "https://example.com"),
        )

        val sent = Email.sent!!
        assertEquals(2, sent.size, "One email for each language")

        val catalan = sent.single { it.to.size == 2 }
        assertEquals(listOf("a@example.com", "c@example.com"), catalan.to.map { it.email })
        assertEquals("Nou lloguer d'espai (#7)", catalan.subject)
        assertTrue("""lang="ca"""" in catalan.htmlContent)
        assertTrue("Alice ha reservat un espai." in catalan.htmlContent, catalan.htmlContent)

        val spanish = sent.single { it.to.size == 1 }
        assertEquals("Nuevo alquiler de espacio (#7)", spanish.subject)
        assertTrue("Alice ha reservado un espacio." in spanish.htmlContent, spanish.htmlContent)
    }

    @Test
    fun `a language without translations falls back to english`() = runTest {
        Email.sendTemplate(
            recipients = listOf(recipient("a@example.com", "ta")),
            template = EmailTemplate.NewSpaceLending,
            args = mapOf("id" to "7", "userName" to "Alice"),
        )
        assertEquals("New space lending (#7)", Email.sent!!.single().subject)
    }

    @Test
    fun `attachments go with the email`() = runTest {
        val attachment = org.centrexcursionistalcoi.app.notifications.email.mailersend.MailerSendAttachment(byteArrayOf(1, 2, 3), "photo.png")
        Email.sendTemplate(
            recipients = listOf(recipient("a@example.com", "en")),
            template = EmailTemplate.NewMemoryUpload,
            args = mapOf("id" to "7", "userName" to "Alice"),
            attachments = listOf(attachment),
        )
        assertEquals(listOf(attachment), Email.sent!!.single().attachments)
    }
}
