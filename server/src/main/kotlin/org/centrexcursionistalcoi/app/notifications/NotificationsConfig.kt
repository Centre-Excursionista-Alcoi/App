package org.centrexcursionistalcoi.app.notifications

import org.centrexcursionistalcoi.app.ConfigProvider

object NotificationsConfig : ConfigProvider() {
    val emailFromAddr get() = getenv("EMAIL_FROM_ADDR") ?: "noreply@centrexcursionistalcoi.org"
    val emailFromName get() = getenv("EMAIL_FROM_NAME") ?: "Centre Excursionista d'Alcoi"
    val emailReplyToAddr get() = getenv("EMAIL_REPLY_TO_ADDR")
    val emailReplyToName get() = getenv("EMAIL_REPLY_TO_NAME")

    val mailerSendToken get() = getenv("MAILER_SEND_TOKEN") ?: error("MAILER_SEND_TOKEN is not set")

    val smtpHost get() = getenv("SMTP_HOST")
    val smtpPort get() = (getenv("SMTP_PORT") ?: "587").toIntOrNull()
    val smtpUsername get() = getenv("SMTP_USER")
    val smtpPassword get() = getenv("SMTP_PASS")
    val smtpUseTls get() = getenv("SMTP_TLS")?.toBoolean() ?: false
}
