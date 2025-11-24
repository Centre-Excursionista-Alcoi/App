package org.centrexcursionistalcoi.app.notifications

import java.util.Locale
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
     * Template for lost password email.
     *
     * Required arguments:
     * - `userName`: The name of the user.
     * - `resetLink`: The link to reset the password.
     */
    object LostPassword : EmailTemplate("lost_password") {
        override fun DocumentRedactor.render(args: Map<String, String?>): String {
            return """
            <html>
                <body>
                    <p>${t("line_1", args["userName"])}</p>
                    <p>${t("line_2")}</p>
                    <p>${t("line_3")}</p>
                    <p><a href="${args["resetLink"]}">${t("line_4")}</a></p>
                    <p>${t("line_5")}</p>
                    <p>${t("line_6")}</p>
                </body>
            </html>
            """.trimIndent()
        }
    }

    object PasswordChangedNotification : EmailTemplate("password_changed") {
        override fun DocumentRedactor.render(args: Map<String, String?>): String {
            return """
            <html>
                <body>
                    <p>${t("line_1", args["userName"])}</p>
                    <p>${t("line_2")}</p>
                    <p>${t("line_3")}</p>
                    <p>${t("line_4")}</p>
                </body>
            </html>
            """.trimIndent()
        }
    }
}
