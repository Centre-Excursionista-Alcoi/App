package org.centrexcursionistalcoi.app.routes

import io.ktor.http.ContentType
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respondText
import java.util.Locale
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import org.centrexcursionistalcoi.app.translation.DocumentRedactor
import org.centrexcursionistalcoi.app.translation.Template
import org.centrexcursionistalcoi.app.translation.locale
import org.centrexcursionistalcoi.app.utils.escapeHtml

@ExperimentalXmlUtilApi
abstract class WebTemplate(name: String): Template("web", name) {
    fun title(locale: Locale): String {
        val list = translationsBook[locale]
        return list["title"]
    }

    abstract class ResourceWebTemplate(private val name: String): WebTemplate(name) {
        override fun DocumentRedactor.render(args: Map<String, String?>): String {
            val resourceName = "web/$name.html"
            val resourceStream = javaClass.classLoader.getResourceAsStream(resourceName)
                ?: throw IllegalStateException("Resource not found: $resourceName")
            val htmlDocument = resourceStream.bufferedReader().use { it.readText() }
            // Translations are specified in static HTML files as `{{key}}` placeholders, which are replaced with
            // the translated text for the current locale -- or, if the caller passed a value under that same key
            // in [args] (for whatever a translation file can't hold, like a per-request URL), that instead.
            // `{{{key}}}` (triple braces, Mustache-style) is the same lookup but inserted verbatim, unescaped --
            // only for a value the caller built itself as real markup (see AppLinkFallbackRoutes' auto_open_meta);
            // every plain `{{key}}` is HTML-escaped, since some of these values (e.g. preview_url) embed the raw
            // request path, which is attacker-controlled.
            return htmlDocument.replace(Regex("\\{\\{\\{(.*?)\\}\\}\\}|\\{\\{(.*?)\\}\\}")) { matchResult ->
                val rawKey = matchResult.groups[1]?.value
                val key = (rawKey ?: matchResult.groups[2]!!.value).trim()
                val value = args[key] ?: translationsBook[locale].getOrNull(key)
                // If neither has it, keep the original placeholder.
                when {
                    value == null -> matchResult.value
                    rawKey != null -> value
                    else -> value.escapeHtml()
                }
            }
        }
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
        (if (args["success"] == "true") {
            """
            <p>${t("success")}</p>
            """.trimIndent()
        } else {
            """
            <form method="POST" action="/reset_password">
                <input type="hidden" name="request_id" value="${args["requestId"] ?: ""}"/>
                <input type="hidden" name="webui" value="true"/>
                <input type="password" id="password" name="password" autocomplete="new-password" required minlength="8" aria-describedby="password-requirements password-error" />
                <label for="password">${t("message")}</label>
                <p id="password-requirements">${t("requirements")}</p>
                <p id="password-error" role="alert" style="color: red">${args["error"] ?: ""}</p>
                <button type="submit">${t("submit")}</button>
            </form>
            """.trimIndent()
        }) +
        """
        </body>
        </html>
        """.trimIndent()
    }

    /**
     * The "get the app" page: whoever lands here is on a desktop or another platform where there's nothing to
     * auto-redirect to (see `respondAppLinkFallback`, which handles Android/iOS by redirecting before this ever
     * renders). Static markup (`web/get_app.html`) plus its own stylesheet (`web/app-links.css`) -- nothing here
     * is per-request, so a plain [ResourceWebTemplate] is all it needs.
     */
    object GetApp : ResourceWebTemplate("get_app")

    companion object {
        suspend fun ApplicationCall.respondTemplate(template: Template, args: Map<String, String?>) {
            val locale = request.locale()
            val htmlContent = template.render(locale, args)
            respondText(htmlContent, ContentType.Text.Html)
        }
    }
}
