package org.centrexcursionistalcoi.app.notifications

import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import org.centrexcursionistalcoi.app.notifications.email.EmailProvider
import org.centrexcursionistalcoi.app.notifications.email.mailersend.MailerSendAttachment
import org.centrexcursionistalcoi.app.notifications.email.mailersend.MailerSendEmail
import org.jetbrains.annotations.TestOnly
import org.slf4j.LoggerFactory

object Email {
    private val logger = LoggerFactory.getLogger("Email")

    private val provider: EmailProvider? by lazy { EmailProvider.providers.firstOrNull { it.isConfigured } }

    @TestOnly
    var disabled: Boolean = false

    fun launch(block: suspend () -> Unit): Job {
        return CoroutineScope(Dispatchers.IO).launch {
            if (disabled) return@launch
            // Emails recorded instead of sent (see [sent]) don't need a provider
            if (sent == null && provider == null) return@launch
            try {
                block()
            } catch (e: Exception) {
                logger.error("Could not invoke email block.", e)
            }
        }
    }

    fun isConfigured() = provider?.isConfigured == true

    suspend fun isAvailable(): Boolean {
        return provider?.isAvailable() == true
    }

    /**
     * The emails that would have been sent, for tests: when set, emails are recorded here instead of sent.
     */
    @TestOnly
    var sent: MutableList<SentEmail>? = null

    class SentEmail(val to: List<MailerSendEmail>, val subject: String, val htmlContent: String, val attachments: List<MailerSendAttachment>?)

    suspend fun sendEmail(to: List<MailerSendEmail>, subject: String, htmlContent: String, attachments: List<MailerSendAttachment>? = null) {
        sent?.let {
            it += SentEmail(to, subject, htmlContent, attachments)
            return
        }
        provider?.sendEmail(to, subject, htmlContent, attachments)
    }

    @ExperimentalXmlUtilApi
    suspend fun sendTemplate(
        to: List<MailerSendEmail>,
        template: EmailTemplate,
        args: Map<String, String?>,
        locale: Locale,
        attachments: List<MailerSendAttachment>? = null,
    ) {
        val subject = template.subject(locale, args)
        val textHtml = template.render(locale, args)
        sendEmail(to, subject, textHtml, attachments)
    }

    /**
     * Sends [template] to each of [recipients], in their own language: one email for each of the languages.
     */
    @ExperimentalXmlUtilApi
    suspend fun sendTemplate(
        recipients: List<EmailRecipient>,
        template: EmailTemplate,
        args: Map<String, String?>,
        attachments: List<MailerSendAttachment>? = null,
    ) {
        for ((locale, group) in recipients.groupBy { it.locale }) {
            sendTemplate(group.map { it.email }, template, args, locale, attachments)
        }
    }
}
