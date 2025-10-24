package org.centrexcursionistalcoi.app.notifications

import io.ktor.client.HttpClient
import io.ktor.client.engine.java.Java
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.mailersend.MailerSendAttachment
import org.centrexcursionistalcoi.app.mailersend.MailerSendEmail
import org.centrexcursionistalcoi.app.mailersend.SendMailRequest

object Email {
    private val httpClient = HttpClient(Java) {
        install(ContentNegotiation) {
            json(json)
        }
        install(Logging) {
            level = LogLevel.HEADERS
        }
        defaultRequest {
            url("https://api.mailersend.com")
            bearerAuth(NotificationsConfig.mailerSendToken)
        }
    }

    suspend fun sendEmail(to: List<MailerSendEmail>, subject: String, htmlContent: String, attachments: List<MailerSendAttachment>? = null) {
        try {
            NotificationsConfig.mailerSendToken
        } catch (_: IllegalStateException) {
            // email not configured
            return
        }

        val request = SendMailRequest(
            from = MailerSendEmail(
                email = NotificationsConfig.emailFromAddr,
                name = NotificationsConfig.emailFromName,
            ),
            to = to,
            subject = subject,
            html = htmlContent,
            replyTo = NotificationsConfig.emailReplyToAddr?.let {
                MailerSendEmail(it, NotificationsConfig.emailReplyToName ?: "")
            },
            attachments = attachments,
        )
        val response = httpClient.post("/v1/email") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        if (!response.status.isSuccess()) {
            if (response.status == HttpStatusCode.UnprocessableEntity) {
                throw IllegalArgumentException("Email could not be sent (UnprocessableEntity): ${response.bodyAsText()}")
            } else {
                throw IllegalStateException("Failed to send email (${response.status}): ${response.bodyAsText()}")
            }
        }
    }
}
