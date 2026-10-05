package org.centrexcursionistalcoi.app.translation

import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.html.a
import kotlinx.html.p
import kotlinx.html.unsafe
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import org.centrexcursionistalcoi.app.notifications.EmailTemplate
import org.centrexcursionistalcoi.app.routes.WebTemplate

@OptIn(ExperimentalXmlUtilApi::class)
class TestHtmlTemplates {
    private val email = object : EmailTemplate("example") {
        override fun DocumentRedactor.render(args: Map<String, String?>): String = email {
            p { +t("greeting", args["userName"]) }
            p { tr("message") }
            args["link"]?.let { link -> p { a(href = link) { +"Open" } } }
        }
    }

    private val page = object : WebTemplate("example") {
        override fun DocumentRedactor.render(args: Map<String, String?>): String = page {
            p { tr("message", args["name"]) }
        }
    }

    @Test
    fun `an email is a document with its body`() {
        assertEquals(
            """<!doctype html><html lang="en"><body><p>Welcome, Alice</p><p>This is a test email.</p></body></html>""",
            email.render(Locale.ENGLISH, mapOf("userName" to "Alice")),
        )
    }

    @Test
    fun `the language of the document and the texts are the ones of the locale`() {
        assertEquals(
            """<!doctype html><html lang="ca"><body><p>Benvingut, Josep</p><p>Aquest és un correu electrònic de prova.</p></body></html>""",
            email.render(Locale.forLanguageTag("ca"), mapOf("userName" to "Josep")),
        )
    }

    @Test
    fun `text is escaped, also what comes from the user`() {
        val html = email.render(Locale.ENGLISH, mapOf("userName" to """<script>alert("x")</script> & co"""))

        assertFalse("<script>" in html, "The name must not become markup: $html")
        assertTrue("&lt;script&gt;" in html, html)
        assertTrue("&amp; co" in html, html)
    }

    @Test
    fun `attributes are escaped`() {
        val html = email.render(Locale.ENGLISH, mapOf("userName" to "Alice", "link" to """https://example.com/?a=1&b="2"><script>"""))

        assertFalse("<script>" in html, html)
        assertTrue("""href="https://example.com/?a=1&amp;b=&quot;2&quot;&gt;&lt;script&gt;"""" in html, html)
    }

    @Test
    fun `markup can be added as it is on purpose`() {
        val template = object : EmailTemplate("example") {
            override fun DocumentRedactor.render(args: Map<String, String?>): String = email {
                p { unsafe { +"<b>bold</b>" } }
            }
        }
        assertTrue("<p><b>bold</b></p>" in template.render(Locale.ENGLISH, emptyMap()))
    }

    @Test
    fun `a page has its translated title`() {
        assertEquals(
            """<!doctype html><html lang="en"><head><title>Example page</title></head><body><p>Hello, Alice</p></body></html>""",
            page.render(Locale.ENGLISH, mapOf("name" to "Alice")),
        )
    }
}
