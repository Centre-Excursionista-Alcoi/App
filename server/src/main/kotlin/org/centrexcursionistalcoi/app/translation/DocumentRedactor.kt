package org.centrexcursionistalcoi.app.translation

import java.util.Locale
import kotlinx.html.FlowOrPhrasingContent
import kotlinx.html.HTML
import kotlinx.html.html
import kotlinx.html.stream.createHTML

/**
 * What a [Template] renders with: the translations for the locale, and a way to build an HTML document.
 *
 * ```
 * override fun DocumentRedactor.render(args: Map<String, String?>) = html {
 *     body {
 *         p { +t("greeting", args["userName"]) }
 *         p { tr("message") }
 *     }
 * }
 * ```
 *
 * Text added with `+` (or [tr]) is escaped. To add markup as it is, use `unsafe { +markup }`, only for markup that
 * was built here: never for an argument.
 */
class DocumentRedactor(val locale: Locale, private val list: TranslationsList) {
    fun t(key: String, vararg formatArgs: Any?) = list.get(key, *formatArgs)

    /**
     * The translation of [key], formatted with [formatArgs], as the (escaped) text of this element.
     */
    fun FlowOrPhrasingContent.tr(key: String, vararg formatArgs: Any?) {
        +t(key, *formatArgs)
    }

    /**
     * An HTML document built with [block]: with its doctype, and the language of the locale.
     *
     * It isn't indented, so it renders as it is written (emails mustn't get whitespace between elements).
     */
    fun html(block: HTML.() -> Unit): String =
        "<!doctype html>" + createHTML(prettyPrint = false).html {
            attributes["lang"] = locale.toLanguageTag()
            block()
        }
}
