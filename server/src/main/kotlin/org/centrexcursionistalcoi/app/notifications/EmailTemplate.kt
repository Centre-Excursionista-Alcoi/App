package org.centrexcursionistalcoi.app.notifications

import java.util.Locale
import kotlinx.html.BODY
import kotlinx.html.FlowContent
import kotlinx.html.a
import kotlinx.html.body
import kotlinx.html.br
import kotlinx.html.li
import kotlinx.html.p
import kotlinx.html.strong
import kotlinx.html.style
import kotlinx.html.ul
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
     * What the `subject` translation is formatted with, from the arguments of the email (like its `%1$s`).
     */
    protected open fun subjectArguments(args: Map<String, String?>): List<Any?> = emptyList()

    /**
     * The subject of the email, in [locale], for these arguments.
     */
    fun subject(locale: Locale, args: Map<String, String?>): String =
        translationsBook[locale].get("subject", *subjectArguments(args).toTypedArray())

    /**
     * The `label: value` rows of the details of something. A missing or blank value is shown as [empty].
     */
    protected fun FlowContent.details(empty: String, vararg rows: Pair<String, String?>) {
        p {
            for ((label, value) in rows) {
                strong { +"$label: " }
                +(value?.takeIf { it.isNotBlank() } ?: empty)
                br()
            }
        }
    }

    /**
     * A button to open [url], which is also shown as a link under it for the clients that don't show buttons.
     */
    protected fun FlowContent.openInApp(url: String, label: String) {
        p {
            a(href = url) {
                style = "display: inline-block; padding: 10px 16px; background: #1b5e20; color: #ffffff; text-decoration: none; border-radius: 4px;"
                +label
            }
        }
        p { a(href = url) { +url } }
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

    /**
     * A new lending of material was requested. For the people who manage lendings.
     *
     * Required arguments:
     * - `id`: The id of the lending.
     * - `userName`: The name of the user who requested it.
     * - `from`, `to`: The dates.
     * - `link`: The link to open the lending in the app.
     * Optional arguments:
     * - `notes`: The notes of the user.
     * - `items`: The items requested, one per line.
     */
    object NewLendingRequest : EmailTemplate("new_lending_request") {
        override fun subjectArguments(args: Map<String, String?>) = listOf(args["id"])

        override fun DocumentRedactor.render(args: Map<String, String?>): String = email {
            p { +t("line_1", args["userName"]) }
            details(
                t("none"),
                t("label_from") to args["from"],
                t("label_to") to args["to"],
                t("label_notes") to args["notes"],
            )
            p {
                strong { +"${t("label_items")}: " }
                ul {
                    for (item in args["items"].orEmpty().lines().filter { it.isNotBlank() }) li { +item }
                }
            }
            p { +t("line_2") }
            openInApp(args["link"].orEmpty(), t("open_in_app"))
        }
    }

    /**
     * The memory of a lending was submitted. For the people who manage lendings.
     *
     * Required arguments:
     * - `id`: The id of the lending.
     * - `userName`: The name of the user who submitted it.
     * - `from`, `to`: The dates of the lending.
     * - `link`: The link to open the lending in the app.
     * Optional arguments:
     * - `notes`: The notes of the lending.
     */
    object NewMemoryUpload : EmailTemplate("new_memory_upload") {
        override fun subjectArguments(args: Map<String, String?>) = listOf(args["id"])

        override fun DocumentRedactor.render(args: Map<String, String?>): String = email {
            p { +t("line_1", args["id"], args["userName"]) }
            details(
                t("none"),
                t("label_from") to args["from"],
                t("label_to") to args["to"],
                t("label_notes") to args["notes"],
            )
            p { +t("line_2") }
            openInApp(args["link"].orEmpty(), t("open_in_app"))
        }
    }

    /**
     * A space lending was registered. For the user who made it.
     *
     * Required arguments:
     * - `userName`: The name of the user.
     * - `spaceName`: The name of the space.
     * - `checkIn`, `checkOut`: The dates.
     * - `price`: The price, as text.
     * - `link`: The link to open the lending in the app.
     * Optional arguments:
     * - `notes`: The notes of the user.
     */
    object SpaceLendingConfirmation : EmailTemplate("space_lending_confirmation") {
        override fun DocumentRedactor.render(args: Map<String, String?>): String = email {
            p { +t("line_1", args["userName"]) }
            details(
                t("none"),
                t("label_space") to args["spaceName"],
                t("label_check_in") to args["checkIn"],
                t("label_check_out") to args["checkOut"],
                t("label_price") to args["price"],
                t("label_notes") to args["notes"],
            )
            p { +t("line_2") }
            p { +t("line_3") }
            openInApp(args["link"].orEmpty(), t("open_in_app"))
        }
    }

    /**
     * A space lending was registered. For the people who manage them.
     *
     * Required arguments:
     * - `id`: The id of the lending.
     * - `userName`: The name of the user who made it.
     * - `spaceName`: The name of the space.
     * - `checkIn`, `checkOut`: The dates.
     * - `price`: The price, as text.
     * - `link`: The link to open the lending in the app.
     * Optional arguments:
     * - `notes`: The notes of the user.
     */
    object NewSpaceLending : EmailTemplate("new_space_lending") {
        override fun subjectArguments(args: Map<String, String?>) = listOf(args["id"])

        override fun DocumentRedactor.render(args: Map<String, String?>): String = email {
            p { +t("line_1", args["userName"]) }
            details(
                t("none"),
                t("label_space") to args["spaceName"],
                t("label_check_in") to args["checkIn"],
                t("label_check_out") to args["checkOut"],
                t("label_price") to args["price"],
                t("label_notes") to args["notes"],
            )
            openInApp(args["link"].orEmpty(), t("open_in_app"))
        }
    }
}
