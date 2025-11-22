package org.centrexcursionistalcoi.app.notifications

import java.util.Locale
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import org.centrexcursionistalcoi.app.translation.TranslationsBook
import org.centrexcursionistalcoi.app.translation.TranslationsList

@ExperimentalXmlUtilApi
abstract class EmailTemplate(name: String) {
    protected val translationsBook = TranslationsBook("email", name)

    fun subject(locale: Locale): String {
        val list = translationsBook[locale]
        return list["subject"]
    }

    fun render(locale: Locale, args: Map<String, String>): String {
        val list = translationsBook[locale]
        val redactor = EmailRedactor(locale, list)
        return redactor.render(args)
    }

    protected abstract fun EmailRedactor.render(args: Map<String, String>): String

    protected class EmailRedactor(val locale: Locale, private val list: TranslationsList) {
        fun t(key: String, vararg formatArgs: Any?) = list.get(key, *formatArgs)
    }

    /**
     * Template for lost password email.
     *
     * Required arguments:
     * - `userName`: The name of the user.
     * - `resetLink`: The link to reset the password.
     */
    object LostPassword : EmailTemplate("lost_password") {
        override fun EmailRedactor.render(args: Map<String, String>): String {
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
}
