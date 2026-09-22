package org.centrexcursionistalcoi.app.routes

import io.ktor.http.ContentType
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respondText
import java.net.URLEncoder
import java.util.Locale
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import org.centrexcursionistalcoi.app.AppLinks
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
     * The landing page at an [org.centrexcursionistalcoi.app.applink.AppLinkRoutes.APP_ONLY] path: whoever lands
     * here has no app installed, or is in a context (an in-app browser, say) that skipped the OS's own App
     * Link/Universal Link interception -- a device that has the app never reaches this at all.
     *
     * Optional arguments:
     * - `userAgent`: The request's `User-Agent` header, used to pick Android/iOS/neither.
     * - `path`: The request's own path and query (`call.request.uri`), used to build the Android launch link;
     *   falls back to the bare path this page is served at (still valid, just drops the query/id).
     */
    object GetTheApp : WebTemplate("get_the_app") {
        override fun DocumentRedactor.render(args: Map<String, String?>): String {
            val userAgent = args["userAgent"].orEmpty()
            val isAndroid = userAgent.contains("Android", ignoreCase = true)
            val isIOS = Regex("iPhone|iPad|iPod", RegexOption.IGNORE_CASE).containsMatchIn(userAgent)
            val appStoreUrl = AppLinks.appStoreUrl

            val primaryButton = when {
                isAndroid -> {
                    // Relaunches this same link through Chrome's "intent://" scheme: it opens the app directly if
                    // it's installed, or follows the fallback to the Play Store if not -- one button, both cases.
                    val hostAndPath = AppLinks.baseUrl.substringAfter("://") + (args["path"] ?: "/")
                    val fallback = URLEncoder.encode(AppLinks.playStoreUrl, "UTF-8")
                    val launchUrl = "intent://$hostAndPath#Intent;scheme=https;package=${AppLinks.ANDROID_PACKAGE_NAME};S.browser_fallback_url=$fallback;end"
                    """<a class="cta" href="$launchUrl">${t("android_cta")}</a>"""
                }
                isIOS && appStoreUrl != null -> """<a class="cta" href="$appStoreUrl">${t("ios_cta")}</a>"""
                else -> ""
            }
            val secondaryLine = when {
                isAndroid -> """<a class="secondary" href="${AppLinks.playStoreUrl}">${t("android_store")}</a>"""
                isIOS && appStoreUrl != null -> """<p class="hint">${t("ios_hint")}</p>"""
                else -> ""
            }
            // iOS's own mechanism for "you already have this app" -- shown as a native banner by Safari, alongside
            // (not instead of) the button above, only if the App Store link carries a numeric app id to point it at.
            val smartAppBanner = appStoreUrl
                ?.let { Regex("""id(\d+)""").find(it)?.groupValues?.get(1) }
                ?.let { appId -> """<meta name="apple-itunes-app" content="app-id=$appId">""" }
                .orEmpty()

            return """
            <!doctype html>
            <html lang="en">
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>${t("title")}</title>
                $smartAppBanner
                <style>
                    :root {
                        --primary: #6D5E0F;
                        --on-primary: #FFFFFF;
                        --bg-1: #FDEFC0;
                        --bg-2: #FFF8E1;
                        --surface: #FFFFFF;
                        --text: #201B0A;
                        --text-muted: #6B6248;
                        --shadow: rgba(109, 94, 15, 0.25);
                    }
                    @media (prefers-color-scheme: dark) {
                        :root {
                            --primary: #DBC66E;
                            --on-primary: #3A3000;
                            --bg-1: #1C1806;
                            --bg-2: #2A2308;
                            --surface: #23200F;
                            --text: #F1EAD4;
                            --text-muted: #B9AF8E;
                            --shadow: rgba(0, 0, 0, 0.5);
                        }
                    }
                    * { box-sizing: border-box; }
                    body {
                        margin: 0;
                        min-height: 100vh;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        padding: 24px;
                        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
                        background: radial-gradient(circle at top, var(--bg-2), var(--bg-1));
                        color: var(--text);
                    }
                    .card {
                        width: 100%;
                        max-width: 400px;
                        background: var(--surface);
                        border-radius: 28px;
                        padding: 40px 32px;
                        text-align: center;
                        box-shadow: 0 20px 60px var(--shadow);
                        animation: rise 0.5s ease-out;
                    }
                    @keyframes rise {
                        from { opacity: 0; transform: translateY(16px); }
                        to { opacity: 1; transform: translateY(0); }
                    }
                    .icon {
                        width: 96px;
                        height: 96px;
                        border-radius: 24px;
                        margin-bottom: 20px;
                        box-shadow: 0 8px 24px var(--shadow);
                    }
                    h1 { font-size: 22px; margin: 0 0 8px; }
                    p { color: var(--text-muted); font-size: 15px; line-height: 1.5; margin: 0 0 28px; }
                    .cta {
                        display: inline-block;
                        width: 100%;
                        padding: 15px 24px;
                        border-radius: 999px;
                        background: var(--primary);
                        color: var(--on-primary);
                        font-weight: 600;
                        font-size: 16px;
                        text-decoration: none;
                        box-shadow: 0 10px 24px var(--shadow);
                        transition: transform 0.15s ease, box-shadow 0.15s ease;
                    }
                    .cta:hover, .cta:active { transform: translateY(-2px); box-shadow: 0 14px 30px var(--shadow); }
                    .secondary {
                        display: block;
                        margin-top: 16px;
                        color: var(--primary);
                        font-weight: 600;
                        font-size: 14px;
                        text-decoration: none;
                    }
                    .secondary:hover { text-decoration: underline; }
                    .hint { margin-top: 20px; margin-bottom: 0; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="card">
                    <img class="icon" src="/web/app-icon.png" alt="">
                    <h1>${t("title")}</h1>
                    <p>${if (primaryButton.isEmpty()) t("other_message") else t("subtitle")}</p>
                    $primaryButton
                    $secondaryLine
                </div>
            </body>
            </html>
            """.trimIndent()
        }
    }

    companion object {
        suspend fun ApplicationCall.respondTemplate(template: Template, args: Map<String, String?>) {
            val locale = request.locale()
            val htmlContent = template.render(locale, args)
            respondText(htmlContent, ContentType.Text.Html)
        }
    }
}
