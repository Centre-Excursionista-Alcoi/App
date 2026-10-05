package org.centrexcursionistalcoi.app.notifications

import java.util.Locale
import kotlinx.html.BODY
import kotlinx.html.a
import kotlinx.html.body
import kotlinx.html.p
import kotlinx.html.style
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import org.centrexcursionistalcoi.app.translation.DocumentRedactor
import org.centrexcursionistalcoi.app.translation.Template

@ExperimentalXmlUtilApi
abstract class EmailTemplate(name: String) : Template("email", name) {
    fun subject(locale: Locale): String {
        val list = translationsBook[locale]
        return list["subject"]
    }

    /**
     * The document of an email, with [content] as its body:
     * ```
     * override fun DocumentRedactor.render(args: Map<String, String?>) = email {
     *     p { +t("line_1", args["userName"]) }
     * }
     * ```
     */
    protected fun DocumentRedactor.email(content: BODY.() -> Unit): String = html {
        body { content() }
    }

    /**
     * Template for lost password email.
     *
     * Required arguments:
     * - `userName`: The name of the user.
     * - `resetLink`: The link to reset the password.
     */
    object LostPassword : EmailTemplate("lost_password") {
        override fun DocumentRedactor.render(args: Map<String, String?>): String = email {
            p { +t("line_1", args["userName"]) }
            p { +t("line_2") }
            p { +t("line_3") }
            p { a(href = args["resetLink"]) { +t("line_4") } }
            p { +t("line_5") }
            p { +t("line_6") }
        }
    }

    /**
     * The code that proves the email is the user's, to register.
     *
     * Required arguments:
     * - `userName`: The name of the user.
     * - `code`: The code.
     */
    object RegistrationCode : EmailTemplate("registration_code") {
        override fun DocumentRedactor.render(args: Map<String, String?>): String = email {
            p { +t("line_1", args["userName"]) }
            p { +t("line_2") }
            p {
                style = "font-size: 28px; font-weight: bold; letter-spacing: 6px;"
                args["code"]?.let { +it }
            }
            p { +t("line_3") }
            p { +t("line_4") }
        }
    }

    object PasswordChangedNotification : EmailTemplate("password_changed") {
        override fun DocumentRedactor.render(args: Map<String, String?>): String = email {
            p { +t("line_1", args["userName"]) }
            p { +t("line_2") }
            p { +t("line_3") }
            p { +t("line_4") }
        }
    }
}
