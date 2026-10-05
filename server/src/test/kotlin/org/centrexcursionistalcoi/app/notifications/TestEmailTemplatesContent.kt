package org.centrexcursionistalcoi.app.notifications

import java.io.File
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertTrue
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi

/**
 * Every line a template has in its translations must be in what it renders, in every language it has: a template that
 * forgets one (or a translation added without the template) would send incomplete emails.
 */
@OptIn(ExperimentalXmlUtilApi::class)
class TestEmailTemplatesContent {
    private val templates = mapOf(
        "lost_password" to EmailTemplate.LostPassword,
        "registration_code" to EmailTemplate.RegistrationCode,
        "password_changed" to EmailTemplate.PasswordChangedNotification,
    )

    private val args = mapOf(
        "userName" to "Alice",
        "resetLink" to "https://example.com/reset?request=1&other=2",
        "code" to "123456",
    )

    /** The translation files of [name]: its language, and the file. */
    private fun translationFiles(name: String): List<Pair<Locale, File>> =
        javaClass.classLoader.getResources("translate/email").toList()
            .flatMap { url -> File(url.toURI()).listFiles().orEmpty().toList() }
            .mapNotNull { file ->
                val match = Regex("""$name(?:-([a-z]+))?\.xml""").matchEntire(file.name) ?: return@mapNotNull null
                (match.groupValues[1].takeIf { it.isNotEmpty() }?.let(Locale::forLanguageTag) ?: Locale.ENGLISH) to file
            }

    /** The `line_N` items of a translation file, formatted with the arguments, as plain text. */
    private fun lines(file: File): List<String> =
        Regex("""<item name="line_\d+">(.*?)</item>""").findAll(file.readText())
            .map { it.groupValues[1].replace("%1\$s", args.getValue("userName")).replace("%s", args.getValue("userName")) }
            .toList()

    /** Text as it was before being written in HTML. */
    private fun String.unescapedHtml() = replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"")
        .replace("&#39;", "'").replace("&amp;", "&")

    @Test
    fun `every template renders all of its lines in every language`() {
        for ((name, template) in templates) {
            val files = translationFiles(name)
            assertTrue(files.isNotEmpty(), "No translations found for $name")
            for ((locale, file) in files) {
                val lines = lines(file)
                assertTrue(lines.isNotEmpty(), "No lines in ${file.name}")
                val html = template.render(locale, args).unescapedHtml()
                for (line in lines) {
                    assertTrue(line in html, "${file.name}: \"$line\" is not in the email: $html")
                }
            }
        }
    }

    @Test
    fun `the arguments are in the email`() {
        val html = EmailTemplate.LostPassword.render(Locale.ENGLISH, args).unescapedHtml()
        assertTrue("""href="${args.getValue("resetLink")}"""" in html, html)
        assertTrue(args.getValue("code") in EmailTemplate.RegistrationCode.render(Locale.ENGLISH, args))
    }
}
