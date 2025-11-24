package org.centrexcursionistalcoi.app.routes

import io.ktor.http.ContentType
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respondText
import java.util.Locale
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import org.centrexcursionistalcoi.app.translation.DocumentRedactor
import org.centrexcursionistalcoi.app.translation.Template
import org.centrexcursionistalcoi.app.translation.locale

@ExperimentalXmlUtilApi
abstract class WebTemplate(name: String): Template("web", name) {
    fun title(locale: Locale): String {
        val list = translationsBook[locale]
        return list["title"]
    }

    /**
     * Renders the template with the given arguments. Required arguments:
     * - `requestId`: The ID of the password reset request.
     * Optional arguments:
     * - `error`: An error message to display.
     */
    object LostPassword : WebTemplate("lost_password") {
        override fun DocumentRedactor.render(args: Map<String, String?>): String = """
        <html>
        <head>
            <title>${t("title")}</title>
        </head>
        <body>
        """.trimIndent() +
        if (args["success"] == "true") {
            """
            <p>${t("success")}</p>
            """.trimIndent()
        } else {
            """
            <form method="POST" action="reset_password">
                <input type="hidden" name="request_id" value="${args["requestId"] ?: ""}"/>
                <input type="hidden" name="webui" value="true"/>
                <input type="text" id="password" name="password" />
                <label for="password">${t("message")}</label>
                <p style="color: red">${args["error"] ?: ""}</p>
                <button type="submit">${t("submit")}</button>
            </form>
            """.trimIndent()
        } +
        """
        </body>
        </html>
        """.trimIndent()
    }

    companion object {
        suspend fun ApplicationCall.respondTemplate(template: Template, args: Map<String, String?>) {
            val locale = request.locale()
            val htmlContent = template.render(locale, args)
            respondText(htmlContent, ContentType.Text.Html)
        }
    }
}
