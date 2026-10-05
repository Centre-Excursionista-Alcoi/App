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
        "new_lending_request" to EmailTemplate.NewLendingRequest,
        "new_memory_upload" to EmailTemplate.NewMemoryUpload,
        "space_lending_confirmation" to EmailTemplate.SpaceLendingConfirmation,
        "new_space_lending" to EmailTemplate.NewSpaceLending,
    )

    private val args = mapOf(
        "userName" to "Alice",
        "resetLink" to "https://example.com/reset?request=1&other=2",
        "code" to "123456",
        "id" to "42",
        "from" to "2026-10-09",
        "to" to "2026-10-10",
        "checkIn" to "2026-10-09",
        "checkOut" to "2026-10-10",
        "notes" to "Some notes",
        "items" to "Rope (60 m)\nHelmet",
        "spaceName" to "Casa de la Serra",
        "price" to "12.00 €",
        "link" to "https://example.com/open",
    )

    /** The translation files of [name]: its language, and the file. */
    private fun translationFiles(name: String): List<Pair<Locale, File>> =
        javaClass.classLoader.getResources("translate/email").toList()
            .flatMap { url -> File(url.toURI()).listFiles().orEmpty().toList() }
            .mapNotNull { file ->
                val match = Regex("""$name(?:-([a-z]+))?\.xml""").matchEntire(file.name) ?: return@mapNotNull null
                (match.groupValues[1].takeIf { it.isNotEmpty() }?.let(Locale::forLanguageTag) ?: Locale.ENGLISH) to file
            }

    /**
     * The texts of a translation file (but the subject, which is not in the body), formatted with the arguments, as
     * plain text. `none` is only used for what is missing, so it isn't in these emails.
     */
    private fun lines(file: File): List<String> =
        Regex("""<item name="(\w+)">(.*?)</item>""").findAll(file.readText())
            .filter { it.groupValues[1] != "subject" && it.groupValues[1] != "none" }
            .map { match ->
                match.groupValues[2]
                    // %1$s is the user in all of them but in the memory, where it is %2$s and %1$s is the lending
                    .replace("%2\$s", args.getValue("userName"))
                    .replace("%1\$s", if (match.groupValues[2].contains("%2\$s")) args.getValue("id") else args.getValue("userName"))
                    .replace("%s", args.getValue("userName"))
            }
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
    fun `every template has the subject in every language`() {
        for ((name, template) in templates) {
            for ((locale, file) in translationFiles(name)) {
                val subject = template.subject(locale, args)
                assertTrue(subject.isNotBlank(), "${file.name}: no subject")
                assertTrue("%" !in subject, "${file.name}: the subject was not formatted: $subject")
            }
        }
        assertTrue("#42" in EmailTemplate.NewLendingRequest.subject(Locale.ENGLISH, args))
    }

    @Test
    fun `the arguments are in the email`() {
        val html = EmailTemplate.LostPassword.render(Locale.ENGLISH, args).unescapedHtml()
        assertTrue("""href="${args.getValue("resetLink")}"""" in html, html)
        assertTrue(args.getValue("code") in EmailTemplate.RegistrationCode.render(Locale.ENGLISH, args))
    }
}
