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

object Email {
    private val provider: EmailProvider? = EmailProvider.providers.firstOrNull { it.isConfigured }

    fun launch(block: suspend () -> Unit): Job {
        return CoroutineScope(Dispatchers.IO).launch {
            if (provider == null) return@launch
            block()
        }
    }

    suspend fun sendEmail(to: List<MailerSendEmail>, subject: String, htmlContent: String, attachments: List<MailerSendAttachment>? = null) {
        provider?.sendEmail(to, subject, htmlContent, attachments)
    }

    @ExperimentalXmlUtilApi
    suspend fun sendTemplate(to: List<MailerSendEmail>, template: EmailTemplate, args: Map<String, String>, locale: Locale) {
        val subject = template.subject(locale)
        val textHtml = template.render(locale, args)
        sendEmail(to, subject, textHtml)
    }
}
